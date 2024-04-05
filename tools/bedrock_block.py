import json5

with open("D:/Download/mojang-blocks.json", "r") as f:
    data = json5.load(f)

block_properties = data["block_properties"]
defaults = {}
for v in block_properties:
    name = v["name"]
    values = v["values"]
    defaults[name] = values[0]["value"]
    if isinstance(defaults[name], bool):
        defaults[name] = 'true' if defaults[name] else 'false'

blocks = data["data_items"]
for v in blocks:
    name = v["name"]
    name = name[name.index(":") + 1 :]
    properties = v["properties"]
    if len(properties) == 0:
        print(f"\t['{name}'] = {{}},")
    else:
        print(f"\t['{name}'] = {{")
        for p in properties:
            pn = p["name"]
            print(f"\t\t{{'{pn}', '{defaults[pn]}'}},")
        print("\t},")
