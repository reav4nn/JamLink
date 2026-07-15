#include "ntp-math.h"

namespace jamlink {

ClockSync& ClockSync::instance() {
    static ClockSync inst;
    return inst;
}

void ClockSync::setOffset(int64_t offsetNs) {
    offsetNs_.store(offsetNs, std::memory_order_release);
}

int64_t ClockSync::getOffset() const {
    return offsetNs_.load(std::memory_order_acquire);
}

int64_t ClockSync::toMasterTime(int64_t localNs) const {
    return localNs + getOffset();
}

} // namespace jamlink
