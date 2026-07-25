import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";

if (getApps().length === 0) initializeApp();

const firestore = getFirestore();
const auth = getAuth();
const USERS = "users";
const ACTIVITIES = "activity_logs";
const ROLES = ["ADMIN", "EMPLOYEE"] as const;
type Role = (typeof ROLES)[number];

type EmployeePayload = {
  name: string;
  email: string;
  phone: string;
  role: Role;
  active: boolean;
};

function invalid(message: string): never { throw new HttpsError("invalid-argument", message); }
function asText(value: unknown, field: string): string {
  if (typeof value !== "string" || value.trim().length === 0) invalid(`${field} is required`);
  return value.trim();
}
function asEmail(value: unknown): string {
  const email = asText(value, "email").toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) invalid("email is invalid");
  return email;
}
function asPhone(value: unknown): string {
  const phone = asText(value, "phone");
  const digits = [...phone].filter((character) => /\d/.test(character)).length;
  if (digits < 7 || digits > 15 || !/^[\d+\-()\s]+$/.test(phone)) invalid("phone is invalid");
  return phone;
}
function asRole(value: unknown): Role {
  if (typeof value !== "string" || !ROLES.includes(value as Role)) invalid("role is invalid");
  return value as Role;
}
function asActive(value: unknown): boolean {
  if (typeof value !== "boolean") invalid("active is required");
  return value;
}
function employeePayload(data: unknown): EmployeePayload {
  if (data === null || typeof data !== "object") invalid("request data is invalid");
  const input = data as Record<string, unknown>;
  return {
    name: asText(input.name, "name"), email: asEmail(input.email), phone: asPhone(input.phone),
    role: asRole(input.role), active: asActive(input.active),
  };
}
function sanitized(uid: string, input: EmployeePayload, firstLogin: boolean) {
  return { uid, name: input.name, email: input.email, phone: input.phone, role: input.role, active: input.active, firstLogin };
}
async function requireActiveAdmin(uid: string | undefined): Promise<{ uid: string; name: string }> {
  if (!uid) throw new HttpsError("unauthenticated", "Authentication is required.");
  const snapshot = await firestore.collection(USERS).doc(uid).get();
  if (!snapshot.exists || snapshot.get("active") !== true || snapshot.get("role") !== "ADMIN") {
    throw new HttpsError("permission-denied", "Administrator access is required.");
  }
  return { uid, name: typeof snapshot.get("name") === "string" ? snapshot.get("name") : "Administrator" };
}
async function assertPhoneAvailable(phone: string, excludedUid?: string): Promise<void> {
  const matches = await firestore.collection(USERS).where("phone", "==", phone).limit(2).get();
  if (matches.docs.some((document) => document.id !== excludedUid)) {
    throw new HttpsError("already-exists", "Phone number is already in use.");
  }
}
function activity(id: string, action: string, targetUid: string, caller: { uid: string; name: string }) {
  return {
    id, module: "EMPLOYEES", action, referenceId: targetUid, referenceType: "USER",
    description: action, performedBy: caller.uid, performedByName: caller.name,
    createdAt: FieldValue.serverTimestamp(),
  };
}

export const createEmployee = onCall({ region: "asia-south1" }, async (request) => {
  const caller = await requireActiveAdmin(request.auth?.uid);
  const input = employeePayload(request.data);
  const temporaryPassword = asText((request.data as Record<string, unknown>).temporaryPassword, "temporaryPassword");
  if (temporaryPassword.length < 8) invalid("temporaryPassword must have at least 8 characters");

  await assertPhoneAvailable(input.phone);
  try { await auth.getUserByEmail(input.email); throw new HttpsError("already-exists", "Email is already in use."); }
  catch (error) {
    if (error instanceof HttpsError) throw error;
    if ((error as { code?: string }).code !== "auth/user-not-found") throw new HttpsError("internal", "Unable to validate email.");
  }

  let createdUid: string | undefined;
  try {
    const created = await auth.createUser({ email: input.email, password: temporaryPassword, displayName: input.name, disabled: !input.active });
    createdUid = created.uid;
    const profile = {
      uid: created.uid, ...input, firstLogin: true, designation: "", profileImage: "",
      createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(),
      createdBy: caller.uid, updatedBy: caller.uid,
    };
    const log = firestore.collection(ACTIVITIES).doc();
    await firestore.runTransaction(async (transaction) => {
      const phoneMatches = await transaction.get(
        firestore.collection(USERS).where("phone", "==", input.phone).limit(1),
      );
      if (!phoneMatches.empty) throw new HttpsError("already-exists", "Phone number is already in use.");
      transaction.create(firestore.collection(USERS).doc(created.uid), profile);
      transaction.create(log, activity(log.id, "CREATE", created.uid, caller));
    });
    return sanitized(created.uid, input, true);
  } catch (error) {
    if (createdUid) await auth.deleteUser(createdUid).catch(() => undefined);
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Unable to create employee.");
  }
});

export const updateEmployee = onCall({ region: "asia-south1" }, async (request) => {
  const caller = await requireActiveAdmin(request.auth?.uid);
  if (request.data === null || typeof request.data !== "object") invalid("request data is invalid");
  const data = request.data as Record<string, unknown>;
  const uid = asText(data.uid, "uid");
  const input = employeePayload(data);
  const targetRef = firestore.collection(USERS).doc(uid);
  const target = await targetRef.get();
  if (!target.exists) throw new HttpsError("not-found", "Employee was not found.");
  if (caller.uid === uid && (input.active === false || input.role !== target.get("role"))) {
    throw new HttpsError("permission-denied", "Administrators cannot deactivate or demote themselves.");
  }
  await assertPhoneAvailable(input.phone, uid);
  const previousEmail = target.get("email") as string;
  if (input.email !== previousEmail) {
    try { const existing = await auth.getUserByEmail(input.email); if (existing.uid !== uid) throw new HttpsError("already-exists", "Email is already in use."); }
    catch (error) {
      if (error instanceof HttpsError) throw error;
      if ((error as { code?: string }).code !== "auth/user-not-found") throw new HttpsError("internal", "Unable to validate email.");
    }
  }
  try {
    await auth.updateUser(uid, { email: input.email, displayName: input.name, disabled: !input.active });
    const log = firestore.collection(ACTIVITIES).doc();
    await firestore.runTransaction(async (transaction) => {
      const phoneMatches = await transaction.get(
        firestore.collection(USERS).where("phone", "==", input.phone).limit(2),
      );
      if (phoneMatches.docs.some((document) => document.id !== uid)) {
        throw new HttpsError("already-exists", "Phone number is already in use.");
      }
      transaction.update(targetRef, { ...input, updatedAt: FieldValue.serverTimestamp(), updatedBy: caller.uid });
      transaction.create(log, activity(log.id, "UPDATE", uid, caller));
    });
  } catch (error) {
    if (input.email !== previousEmail) await auth.updateUser(uid, { email: previousEmail }).catch(() => undefined);
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Unable to update employee.");
  }
  return sanitized(uid, input, target.get("firstLogin") === true);
});
