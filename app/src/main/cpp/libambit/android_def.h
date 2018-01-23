/* Added by Mark Kuo for compilation using Android NDK */

#ifndef ANDROID_DEF_H
#define ANDROID_DEF_H

#include <sys/endian.h>

#if _BYTE_ORDER == _LITTLE_ENDIAN

#undef le16toh
#define le16toh(x) (x)
#undef le32toh
#define le32toh(x) (x)
#undef htole16
#define htole16(x) (x)
#undef htole32
#define htole32(x) (x)
#undef htobe16
#define htobe16 __swap16

#endif

#if _BYTE_ORDER == _BIG_ENDIAN

#define le16toh __swap16
#define le32toh __swap32
#define htole16 __swap16
#define htole32 __swap32
#define htobe16(x) (x)

#endif

#endif
