package cut.the.crap.tools

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Kotlin/Native has no dedicated IO dispatcher; Default is the documented substitute.
actual val defaultIoDispatcher: CoroutineDispatcher = Dispatchers.Default
