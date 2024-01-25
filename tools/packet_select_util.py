import os

import json5


def select_all_data(behavior_packs: str, data_type: str) -> dict[str, str]:
    data_entries = {}

    for bp in os.listdir(behavior_packs):
        if os.path.isdir(os.path.join(behavior_packs, bp, data_type)):
            for file in os.listdir(os.path.join(behavior_packs, bp, data_type)):
                if file.endswith(".json"):
                    data_entry = file[:-5]
                    if data_entry not in data_entries:
                        data_entries[data_entry] = []
                    data_entries[data_entry].append(bp)

    choose_data = {}
    for data_entry, bps in data_entries.items():
        experimental_name = None
        for bp in bps:
            if "experimental" in bp:
                experimental_name = bp

        if experimental_name is None:
            choose_data[data_entry] = bps[-1]
        else:
            choose_data[data_entry] = experimental_name

    return choose_data


def select_all_data_and_load(behavior_packs: str, data_type: str) -> dict[str, any]:
    data_entries_keys = select_all_data(behavior_packs, data_type)
    data_entries = {}
    for k, v in data_entries_keys.items():
        with open(os.path.join(behavior_packs, v, data_type, f"{k}.json")) as f:
            data_entries[k] = json5.load(f)

    return data_entries