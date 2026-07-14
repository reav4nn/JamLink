#include "jamlink-engine.h"

namespace jamlink {

std::string ping(const std::string& input) {
    return input + " pong from C++";
}

} // namespace jamlink
