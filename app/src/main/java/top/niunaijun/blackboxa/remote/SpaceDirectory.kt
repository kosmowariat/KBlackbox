package top.niunaijun.blackboxa.remote

import top.niunaijun.blackboxa.data.AppsRepository

class SpaceSummary(val id: Int, val name: String, val appCount: Int)

class SpaceCreation(val space: SpaceSummary?, val error: String?)

/** What the web panel may do with spaces; kept apart from [AppsRepository] so the server can be tested on the JVM. */
interface SpaceDirectory {
    suspend fun list(): List<SpaceSummary>

    suspend fun create(name: String): SpaceCreation
}

class RepositorySpaceDirectory(private val repo: AppsRepository) : SpaceDirectory {

    override suspend fun list() = repo.getUserList().map { SpaceSummary(it.id, it.name, it.appCount) }

    override suspend fun create(name: String): SpaceCreation {
        val result = repo.createUser(name, installGms = false)
        return SpaceCreation(result.user?.let { SpaceSummary(it.id, it.name, it.appCount) }, result.error)
    }
}
