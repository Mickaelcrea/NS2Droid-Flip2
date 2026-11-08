#!/usr/bin/env python3
"""
fix_armv82_pac.py: Strip ARMv8.3 Pointer Authentication (PAC) instructions
from rewritten Nintendo Switch 2 binaries for execution on ARMv8.2 CPUs
(e.g., Qualcomm Snapdragon 870 / 865 / 855 with Cortex-A77 / A76).

On ARMv8.2-A:
- pacibsp / autibsp are NOPs (HINT instruction space).
- retab (0xd65f0fff) and retaa (0xd65f0bff) trigger SIGILL (signal 4, illegal instruction).
- pacia (0xdac101xx) and xpaci (0xdac143xx) trigger SIGILL.
This script converts retab/retaa to standard ret (0xd65f03c0) and non-hint PAC/XPAC to nop (0xd503201f).
"""

import sys, os, glob, shutil

RETAB = b'\xff\x0f\x5f\xd6'
RETAA = b'\xff\x0b\x5f\xd6'
RET   = b'\xc0\x03\x5f\xd6'
NOP   = b'\x1f\x20\x03\xd5'

def patch_pack(pack_dir):
    if not os.path.isdir(pack_dir):
        print(f"[ERROR] Directory not found: {pack_dir}")
        sys.exit(1)

    bins = glob.glob(os.path.join(pack_dir, "*.patched.bin"))
    if not bins:
        print(f"[ERROR] No *.patched.bin files found in {pack_dir}")
        sys.exit(1)

    for bin_path in sorted(bins):
        name = os.path.basename(bin_path)
        with open(bin_path, 'rb') as f:
            data = bytearray(f.read())

        # Determine text segment end from fixups if available
        fixups_path = bin_path.replace('.patched.bin', '.fixups.tsv')
        text_end = len(data)
        if os.path.isfile(fixups_path):
            with open(fixups_path, 'r') as f:
                for line in f:
                    if line.startswith('# TEXT_END'):
                        parts = line.strip().split()
                        if len(parts) >= 3:
                            text_end = int(parts[2], 0)
                        break

        ret_count = 0
        pac_count = 0
        pacga_count = 0

        # Replace retab / retaa with ret in text
        for off in range(0, text_end, 4):
            word = data[off:off+4]
            if word in (RETAB, RETAA):
                data[off:off+4] = RET
                ret_count += 1
            elif (word[3] == 0xDA and word[2] == 0xC1):
                # Non-hint PAC instructions like pacia, xpaci
                data[off:off+4] = NOP
                pac_count += 1
            elif word[3] == 0x9A and (word[2] & 0xE0) == 0xC0 and (word[1] & 0xFC) == 0x30:
                # pacga Rd, Rn, Rm (ARMv8.3-A): convert to eor Rd, Rn, Rm (ARMv8.0)
                w = int.from_bytes(word, 'little')
                rd, rn, rm = w & 31, (w >> 5) & 31, (w >> 16) & 31
                new_w = 0xCA000000 | (rm << 16) | (rn << 5) | rd
                data[off:off+4] = new_w.to_bytes(4, 'little')
                pacga_count += 1

        if ret_count > 0 or pac_count > 0 or pacga_count > 0:
            with open(bin_path, 'wb') as f:
                f.write(data)
            print(f"Patched {name}: {ret_count} retab/retaa -> ret, {pac_count} pac/xpac -> nop, {pacga_count} pacga -> eor")
        else:
            print(f"Skipped {name}: no unsupported PAC instructions found")

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python3 fix_armv82_pac.py <path_to_android_pack>")
        sys.exit(1)
    patch_pack(sys.argv[1])
