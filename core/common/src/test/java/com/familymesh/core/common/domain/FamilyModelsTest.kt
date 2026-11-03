package com.familymesh.core.common.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FamilyModelsTest {
    @Test
    fun familySpaceAcceptsValidValues() {
        val space = FamilySpace("space-1", "My family", 1_000L)
        assertEquals("My family", space.name)
    }

    @Test
    fun familySpaceRejectsBlankName() {
        assertThrows(IllegalArgumentException::class.java) {
            FamilySpace("space-1", "  ", 1L)
        }
    }

    @Test
    fun familyMemberRequiresNonBlankFamilyId() {
        assertThrows(IllegalArgumentException::class.java) {
            FamilyMember("member-1", " ", "Alex", FamilyRole.MEMBER, 1L)
        }
    }

    @Test
    fun familyMemberPreservesRole() {
        val member = FamilyMember("member-1", "space-1", "Alex", FamilyRole.ADMIN, 1_000L)
        assertEquals(FamilyRole.ADMIN, member.role)
    }

    @Test
    fun familyMemberRejectsBlankDisplayName() {
        assertThrows(IllegalArgumentException::class.java) {
            FamilyMember("member-1", "space-1", "  ", FamilyRole.MEMBER, 1L)
        }
    }

    @Test
    fun familyMemberRejectsNegativeJoinTime() {
        assertThrows(IllegalArgumentException::class.java) {
            FamilyMember("member-1", "space-1", "Alex", FamilyRole.MEMBER, -1L)
        }
    }
}
