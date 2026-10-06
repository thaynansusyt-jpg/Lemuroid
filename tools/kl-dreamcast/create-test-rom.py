# Own SH-4 test program; no commercial game or BIOS data.
import struct
import sys
from pathlib import Path
words = []
refs = []
literals = []
def load(reg, value):
    refs.append((len(words), reg, len(literals)))
    words.append(0)
    literals.append(value)
load(0, 0xa05f8044)
load(1, 0x00800005)
words.append(0x2012)
load(0, 0xa05f805c)
load(1, 319 | (479 << 10) | (1 << 20))
words.append(0x2012)
load(0, 0xa5000000)
load(1, 0xf800f800)
words.extend([0x2012, 0xaffd, 0x0009])
if len(words) % 2:
    words.append(0x0009)
for index, reg, literal in refs:
    address = len(words) * 2 + literal * 4
    pc = (index * 2 + 4) & ~3
    words[index] = 0xd000 | (reg << 8) | ((address - pc) // 4)
code = struct.pack('<' + 'H' * len(words), *words) + struct.pack('<' + 'I' * len(literals), *literals)
header = struct.pack('<16sHHIIIIIHHHHHH', b'\x7fELF\x01\x01\x01' + b'\0' * 9,
    2, 42, 1, 0x8c010000, 52, 256 + len(code), 0, 52, 32, 1, 40, 1, 0)
program = struct.pack('<IIIIIIII', 1, 256, 0x8c010000, 0x8c010000, len(code), len(code), 5, 4)
Path(sys.argv[1]).write_bytes(header + program + b'\0' * (256 - len(header) - len(program)) + code + b'\0' * 40)
