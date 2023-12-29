from typing import Callable

import json5
import matplotlib.pyplot as plt
import numpy as np


def make_plot(
    plot,
    data: dict[str, int | list[int]],
    title: str,
    selected: dict[str, list[str | list[str]]],
    name_mapping: dict[str, str] = None,
    log_scale: bool = False,
    percent: bool = False,
    plane_total_count_function: Callable[[dict[str, int]], int] = None,
):
    if name_mapping is None:
        name_mapping = {}

    x = np.arange(data["minHeight"], data["maxHeight"])

    values = {}
    if "biome" in data:
        values = data["biome"]
    elif "block" in data:
        values = data["block"]

    if plane_total_count_function is None:
        plane_total_count_function = lambda v: sum(v.values())

    total_count = [
        plane_total_count_function({k: v[i] for k, v in values.items()})
        for i in range(len(x))
    ]
    max_total_count = max(total_count)

    value_selected = {}

    for selected_key in selected:
        selected_data = selected[selected_key]
        if len(selected_data) == 1:
            value_selected[selected_key] = values[selected_key]
        else:
            value_combined = [0] * len(values[selected_data[1][0]])
            for key in selected_data[1]:
                for i in range(len(value_combined)):
                    value_combined[i] += values[key][i]
            value_selected[selected_key] = value_combined
    if log_scale:
        processed = {
            key: [
                max(
                    value_selected[key][i] / max(1, total_count[i]),
                    0.5 / max_total_count,
                )
                for i in range(len(value_selected[key]))
            ]
            if percent
            else [max(v, 0.5) for v in value_selected[key]]
            for key in value_selected
        }
    else:
        processed = {
            key: [
                value_selected[key][i] / max(1, total_count[i])
                for i in range(len(value_selected[key]))
            ]
            if percent
            else value_selected[key]
            for key in value_selected
        }
    for key in value_selected:
        plot.plot(
            x,
            processed[key],
            label=name_mapping[key] if key in name_mapping else key,
            color=selected[key][0],
        )

    max_count = max([max(processed[key]) for key in processed])

    plot.set_xlim(data["minHeight"] - 16, data["maxHeight"] + 16)
    plot.set_xticks(np.arange(data["minHeight"], data["maxHeight"] + 1, 32))
    if log_scale:
        plot.set_yscale("log")
        plot.set_ylim(
            (
                0.5 / max_total_count if percent else 0.5,
                max_count * 10**1.25 if percent else max_count * 10**1.25,
            )
        )
    else:
        plot.set_ylim(
            (
                0,
                max_count * 1.25 if percent else max_count * 1.25,
            )
        )

    plot.grid()
    plot.set_xlabel("高度")
    plot.set_ylabel("数量 %" if percent else "数量")
    plot.set_title(title)
    plot.legend()
