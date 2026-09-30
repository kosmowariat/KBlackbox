package top.niunaijun.blackboxa.bean

/** Outcome of creating or duplicating a user: [user] is null when it failed, [error] may accompany a created user. */
data class UserCreationResult(val user: UserBean?, val error: String? = null)
