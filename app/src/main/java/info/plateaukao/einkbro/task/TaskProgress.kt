package info.plateaukao.einkbro.task

data class TaskProgress(
    val taskName: String,
    val status: Status,
    val steps: List<StepLine> = emptyList(),
    val finalMarkdown: String? = null,
) {
    enum class Status { Running, Done, Cancelled, Failed }

    data class StepLine(
        val kind: Kind,
        val text: String,
    ) {
        enum class Kind { Info, Tool, Error }
    }
}
