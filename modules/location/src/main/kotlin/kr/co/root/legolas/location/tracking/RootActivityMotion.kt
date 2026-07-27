package kr.co.root.legolas.location.tracking

enum class RootActivityMotion(
    val storageKey: String,
) {
    Still("still"),
    MovingCandidate("moving_candidate"),
    Moving("moving"),
    MovingDegraded("moving_degraded"),
    Unknown("unknown"),
}
