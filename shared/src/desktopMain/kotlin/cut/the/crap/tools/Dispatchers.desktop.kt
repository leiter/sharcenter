package cut.the.crap.tools

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val defaultIoDispatcher: CoroutineDispatcher = Dispatchers.IO
