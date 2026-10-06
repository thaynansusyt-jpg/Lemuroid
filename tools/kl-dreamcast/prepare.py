from pathlib import Path
import sys

root = Path(sys.argv[1])
header = root / 'core/network/net_platform.h'
source = header.read_text()
# The Android-23 libretro build must not require API-24 interface functions.
# Flycast already compiles its Android implementation in core/network/ifaddrs.c.
old_get = '''\tif (__builtin_available(android 24, *))
\t\treturn ::getifaddrs(ifap);
\telse
\t\treturn ::android_getifaddrs(ifap);'''
old_free = '''\tif (__builtin_available(android 24, *))
\t\t::freeifaddrs(ifa);
\telse
\t\t::android_freeifaddrs(ifa);'''
assert old_get in source and old_free in source, 'Pinned Flycast Android network wrapper changed'
source = source.replace(old_get, '\treturn ::android_getifaddrs(ifap);', 1)
source = source.replace(old_free, '\t::android_freeifaddrs(ifa);', 1)
header.write_text(source)
print('Flycast Android uses its bundled interface-address fallback on every supported API')
