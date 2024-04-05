import argparse
import gzip
import struct
from math import *


# NBT Reading ----------------------------------------------------------------------------------------------------------
def read_mutf8(stream: bytes, offset: int) -> tuple[str, int]:  # Java Modified UTF-8
    length = int.from_bytes(stream[offset : offset + 2], "big", signed=False)
    bytearr = stream[offset + 2 : offset + 2 + length]
    chararr = []

    count = 0
    while count < length:
        c = bytearr[count] & 0xFF
        if c > 127:
            break
        count += 1
        chararr.append(chr(c))

    while count < length:
        c = bytearr[count] & 0xFF
        match c >> 4:
            case 0, 1, 2, 3, 4, 5, 6, 7:  # 0xxxxxxx
                count += 1
                chararr.append(chr(c))
            case 12, 13:  # 110x xxxx   10xx xxxx
                count += 2
                if count > length:
                    raise ValueError("Malformed input: partial character at end")
                c2 = bytearr[count - 1]
                if (c2 & 0xC0) != 0x80:
                    raise ValueError(f"Malformed input around byte {count}")
                chararr.append(chr(((c & 0x1F) << 6) | (c2 & 0x3F)))
            case 14:  # 1110 xxxx  10xx xxxx  10xx xxxx
                count += 3
                if count > length:
                    raise ValueError("Malformed input: partial character at end")
                c2 = bytearr[count - 2]
                c3 = bytearr[count - 1]
                if (c2 & 0xC0) != 0x80 or (c3 & 0xC0) != 0x80:
                    raise ValueError(f"Malformed input around byte {count - 1}")
                chararr.append(chr(((c & 0x0F) << 12) | ((c2 & 0x3F) << 6) | (c3 & 0x3F)))
            case _:  # 10xx xxxx,  1111 xxxx
                raise ValueError(f"Malformed input around byte {count}")

    return "".join(chararr), offset + 2 + length


def read_with_tag(tag: int, offset: int, stream: bytes) -> tuple[any, int]:
    match tag:
        case 0:  # TAG_End
            return None, offset
        case 1:  # TAG_Byte
            return stream[offset], offset + 1
        case 2:  # TAG_Short
            return int.from_bytes(stream[offset : offset + 2], "big", signed=True), offset + 2
        case 3:  # TAG_Int
            return int.from_bytes(stream[offset : offset + 4], "big", signed=True), offset + 4
        case 4:  # TAG_Long
            return int.from_bytes(stream[offset : offset + 8], "big", signed=True), offset + 8
        case 5:  # TAG_Float
            return struct.unpack(">f", stream[offset : offset + 4])[0], offset + 4
        case 6:  # TAG_Double
            return struct.unpack(">d", stream[offset : offset + 8])[0], offset + 8
        case 7:  # TAG_Byte_Array
            length = int.from_bytes(stream[offset : offset + 4], "big", signed=False)
            return list(stream[offset + 4 : offset + 4 + length]), offset + 4 + length
        case 8:  # TAG_String
            return read_mutf8(stream, offset)
        case 9:  # TAG_List
            tag = stream[offset]
            length = int.from_bytes(stream[offset + 1 : offset + 5], "big", signed=False)
            values = []
            offset += 5
            for _ in range(length):
                value, offset = read_with_tag(tag, offset, stream)
                values.append(value)
            return values, offset
        case 10:  # TAG_Compound
            values = {}
            while stream[offset] != 0:
                tag = stream[offset]
                name, offset = read_mutf8(stream, offset + 1)
                value, offset = read_with_tag(tag, offset, stream)
                values[name] = value
            return values, offset + 1
        case 11:  # TAG_Int_Array
            length = int.from_bytes(stream[offset : offset + 4], "big", signed=False)
            values = []
            offset += 4
            for _ in range(length):
                value = int.from_bytes(stream[offset : offset + 4], "big", signed=True)
                values.append(value)
                offset += 4
            return values, offset
        case 12:  # TAG_Long_Array
            length = int.from_bytes(stream[offset : offset + 4], "big", signed=False)
            values = []
            offset += 4
            for _ in range(length):
                value = int.from_bytes(stream[offset : offset + 8], "big", signed=True)
                values.append(value)
                offset += 8
            return values, offset
        case _:  # Unknown tag
            raise ValueError(f"Unknown tag: {tag}")


def read_nbt_file(stream: bytes) -> dict[str, any]:
    stream = gzip.decompress(stream)
    if stream[0] != 10:
        raise ValueError("Invalid NBT file")
    name_length = int.from_bytes(stream[1:3], "big", signed=False)
    return read_with_tag(stream[0], 3 + name_length, stream)[0]


# Available Names ------------------------------------------------------------------------------------------------------
def get_available_name(index: int) -> str:
    if index < 26:
        return chr(65 + index)
    elif index < 52:
        return chr(97 + index - 26)
    else:
        return chr(0x4E00 + index - 52)


def convert_block_state(block_state: any) -> str:
    name = block_state["Name"]
    name = name[name.index(":") + 1 :]
    if "Properties" in block_state:
        properties = block_state["Properties"]
        properties = [f"{k}={v}" for k, v in properties.items()]
        properties.sort()
        name += "[" + ",".join(properties) + "]"
    return name


# Structure File -------------------------------------------------------------------------------------------------------
def parse_structure_file(nbt: dict[str, any], palette_sel: str | None) -> tuple[dict[str, str], list[list[list[str]]]]:
    x, y, z = nbt["size"]
    mapping_data = []
    if "palette" in nbt:
        palette = nbt["palette"]
    elif "palettes" in nbt:
        print(f'Structure file has {len(nbt["palettes"])} palettes')
        palette = nbt["palettes"][int(palette_sel) if palette_sel is not None else 0]
    else:
        raise ValueError("No palette found")
    if "blocks" in nbt:
        blocks = nbt["blocks"]
    else:
        raise ValueError("No blocks found")

    for palette_data in palette:
        name = convert_block_state(palette_data)
        mapping_data.append((name, get_available_name(len(mapping_data))))

    structure_list = [[["-" for _ in range(x)] for _ in range(z)] for _ in range(y)]
    for block_data in blocks:
        name = mapping_data[block_data["state"]][1]
        x, y, z = block_data["pos"]
        structure_list[y][z][x] = name

    return dict((v[1], v[0]) for v in mapping_data), structure_list


# Litematic File -------------------------------------------------------------------------------------------------------
def parse_litematic_file(nbt: dict[str, any], sub_region: str | None) -> tuple[dict[str, str], list[list[list[str]]]]:
    regions = nbt["Regions"]
    print(f'Litematic file has regions: {", ".join(regions.keys())}')

    make_regions = []
    if sub_region is not None:
        region_to_make = sub_region.split(",")
        minx, miny, minz = float("inf"), float("inf"), float("inf")
        maxx, maxy, maxz = float("-inf"), float("-inf"), float("-inf")
        for region in region_to_make:
            region_data = regions[region]
            region_pos = region_data["Position"]
            px, py, pz = region_pos["x"], region_pos["y"], region_pos["z"]
            region_size = region_data["Size"]
            rx, ry, rz = region_size["x"], region_size["y"], region_size["z"]
            minx, miny, minz = min(minx, px, px + rx), min(miny, py, py + ry), min(minz, pz, pz + rz)
            maxx, maxy, maxz = max(maxx, px, px + rx), max(maxy, py, py + ry), max(maxz, pz, pz + rz)
            make_regions.append((region_data, rx, ry, rz, px, py, pz))
        ex, ey, ez = maxx - minx, maxy - miny, maxz - minz
        for region_make in make_regions:
            region_make[4] -= minx
            region_make[5] -= miny
            region_make[6] -= minz
    else:
        metadata = nbt["Metadata"]
        enclosing_size = metadata["EnclosingSize"]
        ex, ey, ez = enclosing_size["x"], enclosing_size["y"], enclosing_size["z"]
        for region in regions:
            region_data = regions[region]
            region_size = region_data["Size"]
            rx, ry, rz = region_size["x"], region_size["y"], region_size["z"]
            region_pos = region_data["Position"]
            px, py, pz = region_pos["x"], region_pos["y"], region_pos["z"]
            make_regions.append((region_data, rx, ry, rz, px, py, pz))

    global_mapping = {}
    structure_list = [[["-" for _ in range(ex)] for _ in range(ez)] for _ in range(ey)]
    for region_make in make_regions:
        this_region = region_make[0]
        tx, ty, tz = region_make[1], region_make[2], region_make[3]
        px, py, pz = region_make[4], region_make[5], region_make[6]
        cx, cy, cz = px + tx, py + ty, pz + tz
        px = px if px <= cx else cx + 1
        py = py if py <= cy else cy + 1
        pz = pz if pz <= cz else cz + 1
        emx, emy, emz = abs(tx), abs(ty), abs(tz)
        palette = this_region["BlockStatePalette"]
        palette = [convert_block_state(palette_data) for palette_data in palette]
        for name in palette:
            if name != "air" and name not in global_mapping:
                global_mapping[name] = get_available_name(len(global_mapping))
        palette = [global_mapping[name] if name != "air" else "-" for name in palette]
        bits = max(2, ceil(log(len(palette), 2)))
        block_state_data = this_region["BlockStates"]
        for y in range(emy):
            for z in range(emz):
                for x in range(emx):
                    index = (y * emz + z) * emx + x
                    stream_index = index * bits
                    byte_index = stream_index // 64
                    bit_index = stream_index % 64
                    if 64 - bit_index >= bits:
                        value = (block_state_data[byte_index] >> bit_index) & ((1 << bits) - 1)
                    else:
                        value = (block_state_data[byte_index] >> bit_index) & ((1 << (64 - bit_index)) - 1)
                        value |= (block_state_data[byte_index + 1] & ((1 << (bits - 64 + bit_index)) - 1)) << (
                                64 - bit_index
                        )
                    structure_list[py + y][pz + z][px + x] = palette[value]

    return {v: k for k, v in global_mapping.items() if k != "air"}, structure_list


# Final Convert --------------------------------------------------------------------------------------------------------
def convert_structure_file(mappings: dict[str, str], structure_list: list[list[list[str]]]) -> str:
    structure_str = ";".join(
        [
            ",".join(["".join(structure_list[i][j]) for j in range(len(structure_list[i]))])
            for i in range(len(structure_list))
        ]
    )
    mapping_str = "|".join([f"{k}={v}" for k, v in mappings.items()])
    return f"{{{{Block structure renderer|{mapping_str}|{structure_str}}}}}"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert structure/schematic file to Block Structure Renderer")
    parser.add_argument("file", help="The file to convert", type=argparse.FileType("rb"))
    parser.add_argument("--output", "-o", help="The output file", default="output.txt", type=argparse.FileType("w"))
    parser.add_argument(
        "--type", "-t", help="The type of the file", choices=["structure", "litematic"], default="structure"
    )
    parser.add_argument("--palette", "-p", help="Choose a specific palette in the file to generate", default=None)
    parser.add_argument("--sub-region", "-s", help="Choose a specific region in the file to generate", default=None)
    args = parser.parse_args()

    nbt_file = read_nbt_file(args.file.read())
    mapping, structure = (
        parse_structure_file(nbt_file, args.palette)
        if args.type == "structure"
        else parse_litematic_file(nbt_file, args.sub_region)
    )
    print(convert_structure_file(mapping, structure))
