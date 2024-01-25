import logging
import os

import json5

from packet_select_util import select_all_data_and_load

ROOT = r"E:\MC-Servers\BE1.20.70.20"
OUTPUT_DIR = "output"

if not os.path.exists(OUTPUT_DIR):
    os.mkdir(OUTPUT_DIR)

raw_spawn_rules = select_all_data_and_load(ROOT + "/behavior_packs", "spawn_rules")

spawn_rules = {
    k: v["minecraft:spawn_rules"]["conditions"]
    for k, v in raw_spawn_rules.items()
    if "minecraft:spawn_rules" in v and "conditions" in v["minecraft:spawn_rules"]
}

population_control = {
    k: v["minecraft:spawn_rules"]["description"]["population_control"]
    for k, v in raw_spawn_rules.items()
    if "minecraft:spawn_rules" in v
    and "description" in v["minecraft:spawn_rules"]
    and "population_control" in v["minecraft:spawn_rules"]["description"]
}

spawn_surface = []
spawn_underground = []

spawn_rule_parsed = []
population_control_parsed = {}
for mob, conditions in spawn_rules.items():
    for condition in conditions:
        if "minecraft:weight" not in condition:
            continue
        weight = condition["minecraft:weight"]["default"]
        biome_filter = (
            condition["minecraft:biome_filter"]
            if "minecraft:biome_filter" in condition
            else None
        )

        population_control_data = population_control[mob]
        is_surface = "minecraft:spawns_on_surface" in condition
        is_underground = "minecraft:spawns_underground" in condition

        if "minecraft:herd" in condition:
            herd_data = condition["minecraft:herd"]
            if isinstance(herd_data, dict):
                min_size = herd_data["min_size"]
                max_size = herd_data["max_size"]
            else:
                min_size = herd_data[0]["min_size"]
                max_size = herd_data[0]["max_size"]
                for herd in herd_data[1:]:
                    if herd["min_size"] != min_size or herd["max_size"] != max_size:
                        logging.warning(
                            f"Mob {mob} has different herd size in different conditions."
                        )
        else:
            min_size = 1
            max_size = 1

        # Hack for stray converting to skeleton
        if mob == "stray":
            spawn_rule_parsed.append(
                (
                    "stray",
                    int(weight / 100 * 80),
                    min_size,
                    max_size,
                    biome_filter,
                )
            )
            spawn_rule_parsed.append(
                (
                    "skeleton",
                    int(weight / 100 * 20),
                    min_size,
                    max_size,
                    biome_filter,
                )
            )
            population_control_parsed["stray"] = population_control_data
            if is_surface:
                spawn_surface.append("stray")
            if is_underground:
                spawn_underground.append("stray")
            continue

        if "minecraft:permute_type" in condition:
            logging.warning(f"Mob {mob} has permute type.")
            permute_type = condition["minecraft:permute_type"]
            for permute in permute_type:
                spawn_rule_parsed.append(
                    (
                        permute["entity_type"][10:]
                        if "entity_type" in permute
                        else mob,
                        permute["weight"],
                        min_size,
                        max_size,
                        biome_filter,
                    )
                )
                population_control_parsed[
                    permute["entity_type"][10:] if "entity_type" in permute else mob
                ] = population_control_data
                if is_surface:
                    spawn_surface.append(
                        permute["entity_type"][10:]
                        if "entity_type" in permute
                        else mob
                    )
                if is_underground:
                    spawn_underground.append(
                        permute["entity_type"][10:]
                        if "entity_type" in permute
                        else mob
                    )
        else:
            spawn_rule_parsed.append(
                (
                    mob,
                    weight,
                    min_size,
                    max_size,
                    biome_filter,
                )
            )
            population_control_parsed[mob] = population_control_data
            if is_surface:
                spawn_surface.append(mob)
            if is_underground:
                spawn_underground.append(mob)

spawn_rule_parsed.sort(key=lambda x: x[0])


def check_biome_filter(biome_filters, biome_tags):
    if biome_filters is None:
        return True

    if isinstance(biome_filters, list):
        for sub_filter in biome_filters:
            if not check_biome_filter(sub_filter, biome_tags):
                return False
        return True

    if "any_of" in biome_filters:
        for sub_filter in biome_filters["any_of"]:
            if check_biome_filter(sub_filter, biome_tags):
                return True
        return False

    if "all_of" in biome_filters:
        for sub_filter in biome_filters["all_of"]:
            if not check_biome_filter(sub_filter, biome_tags):
                return False
        return True

    if "test" in biome_filters:
        test = biome_filters["test"]
        if test == "has_biome_tag":
            value = biome_filters["value"]
            operator = biome_filters["operator"]
            if operator == "==":
                return value in biome_tags
            elif operator == "!=":
                return value not in biome_tags
            else:
                logging.warning(f"Unknown operator '{operator}' in biome filter.")
                return False
        if test == "is_snow_covered":
            value = biome_filters["value"]
            operator = biome_filters["operator"]
            if operator == "==":
                return value == ("is_snow_covered" in biome_tags)
            elif operator == "!=":
                return value != ("is_snow_covered" in biome_tags)
            else:
                logging.warning(f"Unknown operator '{operator}' in biome filter.")
                return False

    logging.warning(f"Unknown biome filter '{biome_filters}'.")
    return False


print(json5.dumps(spawn_rule_parsed, indent=4))

biome_spawn_data = {}
for file in os.listdir(os.path.join(ROOT, "definitions/biomes")):
    with open(os.path.join(ROOT, "definitions/biomes", file)) as f:
        biome_data = json5.load(f)
        biome_name = file[:-11]
        biome_spawn_data[biome_name] = {}

        biome_tags = [
            v
            for v in biome_data["minecraft:biome"]["components"]
            if not v.startswith("minecraft:")
        ]
        if "minecraft:tags" in biome_data["minecraft:biome"]["components"]:
            biome_tags += biome_data["minecraft:biome"]["components"]["minecraft:tags"][
                "tags"
            ]

        if (
            "snow_accumulation"
            in biome_data["minecraft:biome"]["components"]["minecraft:climate"]
            and biome_data["minecraft:biome"]["components"]["minecraft:climate"][
                "snow_accumulation"
            ][0]
            > 0
        ):
            biome_tags.append("is_snow_covered")

        for mob, weight, min_size, max_size, biome_filter in spawn_rule_parsed:
            if check_biome_filter(biome_filter, biome_tags):
                population_control_name = population_control_parsed[mob].upper()
                if mob in spawn_surface and population_control_name + "_SURFACE" not in biome_spawn_data[biome_name]:
                    biome_spawn_data[biome_name][population_control_name + "_SURFACE"] = []
                if mob in spawn_underground and population_control_name + "_UNDERGROUND" not in biome_spawn_data[biome_name]:
                    biome_spawn_data[biome_name][population_control_name + "_UNDERGROUND"] = []

                mob_spawn_entry = {
                    "weight": weight,
                    "min_size": min_size,
                    "max_size": max_size,
                }

                if mob in spawn_surface:
                    biome_spawn_data[biome_name][population_control_name + "_SURFACE"].append(
                        (mob, mob_spawn_entry)
                    )
                if mob in spawn_underground:
                    biome_spawn_data[biome_name][population_control_name + "_UNDERGROUND"].append(
                        (mob, mob_spawn_entry)
                    )

        for population_control_name, mobs in biome_spawn_data[biome_name].items():
            mobs.sort(key=lambda x: (-x[1]["weight"], x[0]))


with open(os.path.join(OUTPUT_DIR, "entity_population_control.txt"), "w") as f:
    population_controls = [(k, v) for k, v in population_control_parsed.items()]
    population_controls.sort(key=lambda x: x[1])
    for mob, population_control_name in population_controls:
        f.write(f"\t['{mob}'] = '{population_control_name.upper()}',\n")


allowed_category = [
    "ANIMAL_SURFACE",
    "ANIMAL_UNDERGROUND",
    "MONSTER_SURFACE",
    "MONSTER_UNDERGROUND",
    "WATER_ANIMAL_SURFACE",
    "WATER_ANIMAL_UNDERGROUND",
    "AMBIENT_UNDERGROUND"
]


with open(os.path.join(OUTPUT_DIR, "biome_spawn_data.txt"), "w") as f:
    for k, v in biome_spawn_data.items():
        f.write(f"\t['{k}'] = {{\n")
        population_control_keys = list(v.keys())
        population_control_keys.sort()
        for population_control_name in population_control_keys:
            if population_control_name not in allowed_category:
                continue
            mobs = v[population_control_name]
            f.write(f"\t\t['{population_control_name}'] = {{\n")
            for mob, mob_spawn_entry in mobs:
                f.write(f"\t\t\t{{\n")
                f.write(f"\t\t\t\t['entity_id'] = '{mob}',\n")
                for item_name, item_value in mob_spawn_entry.items():
                    f.write(f"\t\t\t\t['{item_name}'] = {item_value},\n")
                f.write("\t\t\t},\n")
            f.write("\t\t},\n")
        f.write("\t},\n")
