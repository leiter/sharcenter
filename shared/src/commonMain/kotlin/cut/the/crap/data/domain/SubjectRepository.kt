package cut.the.crap.data.domain

import cut.the.crap.data.db.LinkSubjectCrossRef
import cut.the.crap.data.db.PostSubjectCrossRef
import cut.the.crap.data.db.SubjectDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SubjectRepository {
    suspend fun insert(subject: Subject): Long
    suspend fun update(subject: Subject)
    suspend fun delete(subject: Subject)
    suspend fun getById(id: Int): Subject?

    /** All subjects, most-recently-touched first. */
    fun getAllByRecency(): Flow<List<Subject>>

    /** All subjects, alphabetically (name, then colour). */
    fun getAllByName(): Flow<List<Subject>>

    // Post <-> subject membership.
    suspend fun addSubjectToPost(postId: Int, subjectId: Int)
    suspend fun removeSubjectFromPost(postId: Int, subjectId: Int)
    fun getSubjectsForPost(postId: Int): Flow<List<Subject>>

    // Link <-> subject membership.
    suspend fun addSubjectToLink(linkId: Int, subjectId: Int)
    suspend fun removeSubjectFromLink(linkId: Int, subjectId: Int)
    fun getSubjectsForLink(linkId: Int): Flow<List<Subject>>
}

class SubjectRepositoryImpl constructor(
    private val subjectDao: SubjectDao
) : SubjectRepository {

    override suspend fun insert(subject: Subject): Long =
        subjectDao.insert(subject.toDbItem())

    override suspend fun update(subject: Subject) =
        subjectDao.update(subject.copy(modifiedAt = System.currentTimeMillis()).toDbItem())

    override suspend fun delete(subject: Subject) =
        subjectDao.delete(subject.toDbItem())

    override suspend fun getById(id: Int): Subject? =
        subjectDao.getById(id)?.toDomain()

    override fun getAllByRecency(): Flow<List<Subject>> =
        subjectDao.getAllByRecency().map { list -> list.map { it.toDomain() } }

    override fun getAllByName(): Flow<List<Subject>> =
        subjectDao.getAllByName().map { list -> list.map { it.toDomain() } }

    override suspend fun addSubjectToPost(postId: Int, subjectId: Int) =
        subjectDao.linkPost(PostSubjectCrossRef(postId = postId, subjectId = subjectId))

    override suspend fun removeSubjectFromPost(postId: Int, subjectId: Int) =
        subjectDao.unlinkPost(PostSubjectCrossRef(postId = postId, subjectId = subjectId))

    override fun getSubjectsForPost(postId: Int): Flow<List<Subject>> =
        subjectDao.getSubjectsForPost(postId).map { list -> list.map { it.toDomain() } }

    override suspend fun addSubjectToLink(linkId: Int, subjectId: Int) =
        subjectDao.linkLink(LinkSubjectCrossRef(linkId = linkId, subjectId = subjectId))

    override suspend fun removeSubjectFromLink(linkId: Int, subjectId: Int) =
        subjectDao.unlinkLink(LinkSubjectCrossRef(linkId = linkId, subjectId = subjectId))

    override fun getSubjectsForLink(linkId: Int): Flow<List<Subject>> =
        subjectDao.getSubjectsForLink(linkId).map { list -> list.map { it.toDomain() } }
}
