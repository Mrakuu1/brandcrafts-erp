package com.brandcrafts.erp.data.mapper

import com.brandcrafts.erp.data.model.contact.ContactDto
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.ContactInput
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.ContactUpdate
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

fun DocumentSnapshot.toContactDto(): ContactDto = ContactDto(
    id = getString("id") ?: id,
    type = requireNotNull(getString("type")) { "Contact $id is missing type" },
    name = requireNotNull(getString("name")) { "Contact $id is missing name" },
    company = getString("company") ?: "",
    phone = getString("phone") ?: "",
    email = getString("email") ?: "",
    address = getString("address") ?: "",
    gstNumber = getString("gstNumber") ?: "",
    city = getString("city") ?: "",
    state = getString("state") ?: "",
    pincode = getString("pincode") ?: "",
    notes = getString("notes") ?: "",
    active = getBoolean("active") ?: false,
    createdAt = getTimestamp("createdAt"),
    updatedAt = getTimestamp("updatedAt"),
    createdBy = getString("createdBy") ?: "",
    updatedBy = getString("updatedBy") ?: "",
)

fun ContactDto.toDomain(): Contact = Contact(
    id = id,
    type = ContactType.entries.firstOrNull { it.name == type }
        ?: throw IllegalArgumentException("Invalid contact type: $type"),
    name = name,
    company = company,
    phone = phone,
    email = email,
    address = address,
    gstNumber = gstNumber,
    city = city,
    state = state,
    pincode = pincode,
    notes = notes,
    active = active,
    createdAtMillis = createdAt?.toDate()?.time,
    updatedAtMillis = updatedAt?.toDate()?.time,
    createdBy = createdBy,
    updatedBy = updatedBy,
)

fun ContactInput.toContactDto(createdBy: String): ContactDto = ContactDto(
    id = "",
    type = type.name,
    name = name,
    company = company,
    phone = phone,
    email = email,
    address = address,
    gstNumber = gstNumber,
    city = city,
    state = state,
    pincode = pincode,
    notes = notes,
    active = active,
    createdAt = null,
    updatedAt = null,
    createdBy = createdBy,
    updatedBy = createdBy,
)

fun ContactUpdate.toContactDto(updatedBy: String): ContactDto = ContactDto(
    id = id,
    type = "",
    name = name,
    company = company,
    phone = phone,
    email = email,
    address = address,
    gstNumber = gstNumber,
    city = city,
    state = state,
    pincode = pincode,
    notes = notes,
    active = active,
    createdAt = null,
    updatedAt = null,
    createdBy = "",
    updatedBy = updatedBy,
)

fun ContactDto.toCreateMap(id: String): Map<String, Any?> = mapOf(
    "id" to id,
    "type" to type,
    "name" to name,
    "company" to company,
    "phone" to phone,
    "email" to email,
    "address" to address,
    "gstNumber" to gstNumber,
    "city" to city,
    "state" to state,
    "pincode" to pincode,
    "notes" to notes,
    "active" to active,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
    "createdBy" to createdBy,
    "updatedBy" to updatedBy,
)

fun ContactDto.toUpdateMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "company" to company,
    "phone" to phone,
    "email" to email,
    "address" to address,
    "gstNumber" to gstNumber,
    "city" to city,
    "state" to state,
    "pincode" to pincode,
    "notes" to notes,
    "active" to active,
    "updatedAt" to FieldValue.serverTimestamp(),
    "updatedBy" to updatedBy,
)
