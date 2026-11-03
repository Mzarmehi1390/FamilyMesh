package com.familymesh.core.common.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FamilyModelsTest {
    @Test
    fun familySpaceAcceptsValidValues() {
        val space = FamilySpace(id = "space-1", name = "My family", createdAtEpochMillis = 1_000L)
        assertEquals("My family", space.name)
    }

    @Test
    fun familySpaceRejectsBlankName() {
        assertThrows(IllegalArgumentException::class.java) {
            FamilySpace(id = "space-1", name = "  ", createdAtEpochMillis = 1L)
        }
    }

    @Test
    fun familyMemberRequiresNonBlankFamilyId() {
        assertThrows(IllegalArgumentException::class.java) {
            FamilyMember(
                id = "member-1",
                familySpaceId = " ",
                displayName = "Alex",
                role = FamilyRole.MEMBER,
                joinedAtEpochMillis = 1L
            )
        }
    }

    @Test
    fun familyMemberPreservesRole() {
        val member = FamilyMember(
            id = "member-1",
            familySpaceId = "space-1",
            displayName = "Alex",
            role = FamilyRole.ADMIN,
            joinedAtEpochMillis = 1_000L
        )
        assertEquals(FamilyRole.ADMIN, member.role)
    }
}
