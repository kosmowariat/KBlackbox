package top.niunaijun.blackboxa.bean

data class DuplicateUserBean(val source: UserBean, val apps: List<AppInfo>, val googlePackages: Set<String>)
