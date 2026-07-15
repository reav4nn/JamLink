#pragma once
#include <atomic>
#include <cstdint>

namespace jamlink {

class ClockSync {
public:
    static ClockSync& instance();

    void setOffset(int64_t offsetNs);
    int64_t getOffset() const;

    // Convert local monotonic ns to master-aligned ns
    int64_t toMasterTime(int64_t localNs) const;

private:
    std::atomic<int64_t> offsetNs_{0};
};

} // namespace jamlink
