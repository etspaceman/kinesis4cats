package kinesis4cats

sealed trait RecordProcessorState

object RecordProcessorState {
    case object Initialized extends RecordProcessorState
    case object ShardEnded extends RecordProcessorState
    case object Processing extends RecordProcessorState
    case object Shutdown extends RecordProcessorState
    case object LeaseLost extends RecordProcessorState
}
