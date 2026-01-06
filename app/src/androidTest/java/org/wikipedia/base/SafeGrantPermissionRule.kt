package org.wikipedia.base

import androidx.test.rule.GrantPermissionRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A wrapper around GrantPermissionRule that gracefully handles SecurityException
 * which can occur in CI environments when permissions are already granted via adb.
 */
class SafeGrantPermissionRule(private val permissions: Array<out String>) : TestRule {
    private val delegate = GrantPermissionRule.grant(*permissions)

    override fun apply(base: Statement, description: Description): Statement {
        val delegateStatement = delegate.apply(base, description)
        return object : Statement() {
            override fun evaluate() {
                try {
                    delegateStatement.evaluate()
                } catch (e: SecurityException) {
                    // If permission grant fails (e.g., already granted via adb in CI),
                    // check if permission is actually needed and continue
                    if (e.message?.contains("Error granting runtime permission") == true ||
                        e.message?.contains("grantRuntimePermissionAsUser") == true) {
                        // Permission might already be granted, continue with test
                        base.evaluate()
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    companion object {
        fun grant(vararg permissions: String): SafeGrantPermissionRule {
            return SafeGrantPermissionRule(permissions)
        }
    }
}

