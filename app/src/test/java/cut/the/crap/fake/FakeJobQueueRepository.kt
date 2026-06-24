package cut.the.crap.fake

import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.task.FileUploadData
import cut.the.crap.data.rest.task.JobQueueRepository
import cut.the.crap.data.rest.task.Task

/**
 * Fake implementation of JobQueueRepository for testing.
 * Allows configuring success/failure responses for tests.
 */
class FakeJobQueueRepository : JobQueueRepository {

    private var submitTaskResult: Result<String> = Result.Success("Job submitted")
    private var uploadFilesResult: Result<String> = Result.Success("Files uploaded")

    private val submittedTasks = mutableListOf<Task>()
    private val uploadedFiles = mutableListOf<List<FileUploadData>>()

    override suspend fun submitTask(task: Task): Result<String> {
        submittedTasks.add(task)
        return submitTaskResult
    }

    override suspend fun uploadFiles(files: List<FileUploadData>): Result<String> {
        uploadedFiles.add(files)
        return uploadFilesResult
    }

    // Test helpers
    fun setSubmitTaskResult(result: Result<String>) {
        submitTaskResult = result
    }

    fun setUploadFilesResult(result: Result<String>) {
        uploadFilesResult = result
    }

    fun setSubmitTaskSuccess(message: String = "Job submitted") {
        submitTaskResult = Result.Success(message)
    }

    fun setSubmitTaskError(message: String, exception: Throwable? = null) {
        submitTaskResult = Result.Error(message, exception)
    }

    fun setUploadFilesSuccess(message: String = "Files uploaded") {
        uploadFilesResult = Result.Success(message)
    }

    fun setUploadFilesError(message: String, exception: Throwable? = null) {
        uploadFilesResult = Result.Error(message, exception)
    }

    fun getSubmittedTasks(): List<Task> = submittedTasks.toList()

    fun getLastSubmittedTask(): Task? = submittedTasks.lastOrNull()

    fun getUploadedFiles(): List<List<FileUploadData>> = uploadedFiles.toList()

    fun reset() {
        submitTaskResult = Result.Success("Job submitted")
        uploadFilesResult = Result.Success("Files uploaded")
        submittedTasks.clear()
        uploadedFiles.clear()
    }
}
