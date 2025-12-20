import argparse
import copy
import io
import json
import math
import os
import shutil
from functools import cmp_to_key
from typing import Callable
from zipfile import ZipFile

import tqdm
from PIL import Image, ImageDraw

SPECIALS = {
    'water': ['block/water_still', 'block/water_flow', 'block/water_overlay'],
    'lava': ['block/lava_still', 'block/lava_flow'],
    'bell': ['entity/bell/bell_body'],
    'ender_chest': ['entity/chest/ender'],
    'copper_chest': ['entity/chest/copper', 'entity/chest/copper_left', 'entity/chest/copper_right'],
    'exposed_copper_chest': ['entity/chest/copper_exposed', 'entity/chest/copper_exposed_left', 'entity/chest/copper_exposed_right'],
    'weathered_copper_chest': ['entity/chest/copper_weathered', 'entity/chest/copper_weathered_left', 'entity/chest/copper_weathered_right'],
    'oxidized_copper_chest': ['entity/chest/copper_oxidized', 'entity/chest/copper_oxidized_left', 'entity/chest/copper_oxidized_right'],
    'waxed_copper_chest': ['entity/chest/copper', 'entity/chest/copper_left', 'entity/chest/copper_right'],
    'waxed_exposed_copper_chest': ['entity/chest/copper_exposed', 'entity/chest/copper_exposed_left', 'entity/chest/copper_exposed_right'],
    'waxed_weathered_copper_chest': ['entity/chest/copper_weathered', 'entity/chest/copper_weathered_left', 'entity/chest/copper_weathered_right'],
    'waxed_oxidized_copper_chest ': ['entity/chest/copper_oxidized', 'entity/chest/copper_oxidized_left', 'entity/chest/copper_oxidized_right'],
    'chest': ['entity/chest/normal', 'entity/chest/normal_left', 'entity/chest/normal_right'],
    'trapped_chest': ['entity/chest/trapped', 'entity/chest/trapped_left', 'entity/chest/trapped_right'],
    'decorated_pot': ['entity/decorated_pot/decorated_pot_base', 'entity/decorated_pot/decorated_pot_side'],
    'enchanting_table': ['entity/enchanting_table_book'],
    'lectern': ['entity/enchanting_table_book'],
    'shulker_box': ['entity/shulker/shulker'],
    'white_shulker_box': ['entity/shulker/shulker_white'],
    'orange_shulker_box': ['entity/shulker/shulker_orange'],
    'magenta_shulker_box': ['entity/shulker/shulker_magenta'],
    'light_blue_shulker_box': ['entity/shulker/shulker_light_blue'],
    'yellow_shulker_box': ['entity/shulker/shulker_yellow'],
    'lime_shulker_box': ['entity/shulker/shulker_lime'],
    'pink_shulker_box': ['entity/shulker/shulker_pink'],
    'gray_shulker_box': ['entity/shulker/shulker_gray'],
    'light_gray_shulker_box': ['entity/shulker/shulker_light_gray'],
    'cyan_shulker_box': ['entity/shulker/shulker_cyan'],
    'purple_shulker_box': ['entity/shulker/shulker_purple'],
    'blue_shulker_box': ['entity/shulker/shulker_blue'],
    'brown_shulker_box': ['entity/shulker/shulker_brown'],
    'green_shulker_box': ['entity/shulker/shulker_green'],
    'red_shulker_box': ['entity/shulker/shulker_red'],
    'black_shulker_box': ['entity/shulker/shulker_black'],
    'white_banner': ['entity/banner/base'],
    'orange_banner': ['entity/banner/base'],
    'magenta_banner': ['entity/banner/base'],
    'light_blue_banner': ['entity/banner/base'],
    'yellow_banner': ['entity/banner/base'],
    'lime_banner': ['entity/banner/base'],
    'pink_banner': ['entity/banner/base'],
    'gray_banner': ['entity/banner/base'],
    'light_gray_banner': ['entity/banner/base'],
    'cyan_banner': ['entity/banner/base'],
    'purple_banner': ['entity/banner/base'],
    'blue_banner': ['entity/banner/base'],
    'brown_banner': ['entity/banner/base'],
    'green_banner': ['entity/banner/base'],
    'red_banner': ['entity/banner/base'],
    'black_banner': ['entity/banner/base'],
    'white_wall_banner': ['entity/banner/base'],
    'orange_wall_banner': ['entity/banner/base'],
    'magenta_wall_banner': ['entity/banner/base'],
    'light_blue_wall_banner': ['entity/banner/base'],
    'yellow_wall_banner': ['entity/banner/base'],
    'lime_wall_banner': ['entity/banner/base'],
    'pink_wall_banner': ['entity/banner/base'],
    'gray_wall_banner': ['entity/banner/base'],
    'light_gray_wall_banner': ['entity/banner/base'],
    'cyan_wall_banner': ['entity/banner/base'],
    'purple_wall_banner': ['entity/banner/base'],
    'blue_wall_banner': ['entity/banner/base'],
    'brown_wall_banner': ['entity/banner/base'],
    'green_wall_banner': ['entity/banner/base'],
    'red_wall_banner': ['entity/banner/base'],
    'black_wall_banner': ['entity/banner/base'],
    'white_bed': ['entity/bed/white'],
    'orange_bed': ['entity/bed/orange'],
    'magenta_bed': ['entity/bed/magenta'],
    'light_blue_bed': ['entity/bed/light_blue'],
    'yellow_bed': ['entity/bed/yellow'],
    'lime_bed': ['entity/bed/lime'],
    'pink_bed': ['entity/bed/pink'],
    'gray_bed': ['entity/bed/gray'],
    'light_gray_bed': ['entity/bed/light_gray'],
    'cyan_bed': ['entity/bed/cyan'],
    'purple_bed': ['entity/bed/purple'],
    'blue_bed': ['entity/bed/blue'],
    'brown_bed': ['entity/bed/brown'],
    'green_bed': ['entity/bed/green'],
    'red_bed': ['entity/bed/red'],
    'black_bed': ['entity/bed/black'],
    'oak_sign': ['entity/signs/oak'],
    'spruce_sign': ['entity/signs/spruce'],
    'birch_sign': ['entity/signs/birch'],
    'jungle_sign': ['entity/signs/jungle'],
    'acacia_sign': ['entity/signs/acacia'],
    'dark_oak_sign': ['entity/signs/dark_oak'],
    'mangrove_sign': ['entity/signs/mangrove'],
    'cherry_sign': ['entity/signs/cherry'],
    'bamboo_sign': ['entity/signs/bamboo'],
    'crimson_sign': ['entity/signs/crimson'],
    'warped_sign': ['entity/signs/warped'],
    'oak_wall_sign': ['entity/signs/oak'],
    'spruce_wall_sign': ['entity/signs/spruce'],
    'birch_wall_sign': ['entity/signs/birch'],
    'jungle_wall_sign': ['entity/signs/jungle'],
    'acacia_wall_sign': ['entity/signs/acacia'],
    'dark_oak_wall_sign': ['entity/signs/dark_oak'],
    'mangrove_wall_sign': ['entity/signs/mangrove'],
    'cherry_wall_sign': ['entity/signs/cherry'],
    'bamboo_wall_sign': ['entity/signs/bamboo'],
    'crimson_wall_sign': ['entity/signs/crimson'],
    'warped_wall_sign': ['entity/signs/warped'],
    'oak_hanging_sign': ['entity/signs/hanging/oak'],
    'spruce_hanging_sign': ['entity/signs/hanging/spruce'],
    'birch_hanging_sign': ['entity/signs/hanging/birch'],
    'jungle_hanging_sign': ['entity/signs/hanging/jungle'],
    'acacia_hanging_sign': ['entity/signs/hanging/acacia'],
    'dark_oak_hanging_sign': ['entity/signs/hanging/dark_oak'],
    'mangrove_hanging_sign': ['entity/signs/hanging/mangrove'],
    'cherry_hanging_sign': ['entity/signs/hanging/cherry'],
    'bamboo_hanging_sign': ['entity/signs/hanging/bamboo'],
    'crimson_hanging_sign': ['entity/signs/hanging/crimson'],
    'warped_hanging_sign': ['entity/signs/hanging/warped'],
    'oak_wall_hanging_sign': ['entity/signs/hanging/oak'],
    'spruce_wall_hanging_sign': ['entity/signs/hanging/spruce'],
    'birch_wall_hanging_sign': ['entity/signs/hanging/birch'],
    'jungle_wall_hanging_sign': ['entity/signs/hanging/jungle'],
    'acacia_wall_hanging_sign': ['entity/signs/hanging/acacia'],
    'dark_oak_wall_hanging_sign': ['entity/signs/hanging/dark_oak'],
    'mangrove_wall_hanging_sign': ['entity/signs/hanging/mangrove'],
    'cherry_wall_hanging_sign': ['entity/signs/hanging/cherry'],
    'bamboo_wall_hanging_sign': ['entity/signs/hanging/bamboo'],
    'crimson_wall_hanging_sign': ['entity/signs/hanging/crimson'],
    'warped_wall_hanging_sign': ['entity/signs/hanging/warped'],
    'end_portal': ['environment/end_sky', 'entity/end_portal'],
    'end_gateway': ['environment/end_sky', 'entity/end_portal'],
    'skeleton_skull': ['entity/skeleton/skeleton'],
    'wither_skeleton_skull': ['entity/skeleton/wither_skeleton'],
    'zombie_head': ['entity/zombie/zombie'],
    'creeper_head': ['entity/creeper/creeper'],
    'dragon_head': ['entity/enderdragon/dragon'],
    'piglin_head': ['entity/piglin/piglin'],
    'player_head': ['entity/player/slim/steve'],
    'skeleton_wall_skull': ['entity/skeleton/skeleton'],
    'wither_wall_skeleton_skull': ['entity/skeleton/wither_skeleton'],
    'zombie_wall_head': ['entity/zombie/zombie'],
    'creeper_wall_head': ['entity/creeper/creeper'],
    'dragon_wall_head': ['entity/enderdragon/dragon'],
    'piglin_wall_head': ['entity/piglin/piglin'],
    'player_wall_head': ['entity/player/slim/steve'],
    'conduit': ['entity/conduit/base', 'entity/conduit/cage', 'entity/conduit/wind',
                'entity/conduit/wind_vertical', 'entity/conduit/open_eye', 'entity/conduit/closed_eye']
}


class Stitcher:
    def __init__(self, max_width=1024, max_height=1024):
        self.max_width = max_width
        self.max_height = max_height
        self.storage_width = 0
        self.storage_height = 0
        self.wait_stitched: list[tuple[str, Image]] = []
        self.storage: list[Stitcher.Region] = [Stitcher.Region(0, 0, 64, 64)]

    def add_texture(self, name: str, image: Image, zip_file: ZipFile):
        meta_file = f'assets/minecraft/textures/{name.replace('minecraft:', '')}.png.mcmeta'
        if meta_file in zip_file.namelist():
            meta_info = json.loads(zip_file.read(meta_file))
            if 'animation' in meta_info:
                animation = meta_info['animation']
                width_contains = 'width' in animation
                height_contains = 'height' in animation
                width = height = min(image.height, image.width)
                if width_contains and height_contains:
                    width = animation['width']
                    height = animation['height']
                elif width_contains:
                    width = animation['width']
                    height = image.height
                elif height_contains:
                    width = image.width
                    height = animation['height']
                rows = image.height // height
                columns = image.width // width
                for r in range(rows):
                    for c in range(columns):
                        self.wait_stitched.append((
                            f'{name}~{r * columns + c}',
                            image.crop((c * width, r * height, (c + 1) * width, (r + 1) * height))
                        ))
                frames = []
                times = []
                default_time = animation['frametime'] if 'frametime' in animation else 1
                if 'frames' in animation:
                    frames_data = animation['frames']
                    for single in frames_data:
                        if isinstance(single, int):
                            frames.append(single)
                            times.append(default_time)
                        else:
                            frames.append(single['index'])
                            frames.append(single['time'] if 'time' in single else default_time)
                else:
                    for i in range(rows * columns):
                        frames.append(i)
                        times.append(default_time)
                return_data = {
                    'frames': [f'{name}~{i}' for i in frames],
                    'time': times
                }
                if 'interpolate' in animation and animation['interpolate']:
                    return_data['interpolate'] = True
                return return_data
        self.wait_stitched.append((name, image))
        return name

    class Region:
        def __init__(self, x, y, w, h):
            self.x = x
            self.y = y
            self.w = w
            self.h = h
            self.name = None
            self.texture = None
            self.sub_slots: list[Stitcher.Region] | None = None

        def add(self, name: str, texture: Image) -> bool:
            tw = texture.width
            th = texture.height
            w = self.w
            h = self.h
            if self.name is not None:
                return False
            if tw > w or th > h:
                return False
            if tw == w and th == h:
                self.name = name
                self.texture = texture
                return True
            if self.sub_slots is None:
                sub_slots = self.sub_slots = []
                x = self.x
                y = self.y
                sub_slots.append(Stitcher.Region(x, y, tw, th))
                lw = w - tw
                lh = h - th
                if lw > 0 and lh > 0:
                    mw = max(w, lh)
                    mh = max(h, lw)
                    if mh >= mw:
                        sub_slots.append(Stitcher.Region(x, y + th, tw, lh))
                        sub_slots.append(Stitcher.Region(x + tw, y, lw, h))
                    else:
                        sub_slots.append(Stitcher.Region(x + tw, y, lw, th))
                        sub_slots.append(Stitcher.Region(x, y + th, w, lh))
                elif lw == 0:
                    sub_slots.append(Stitcher.Region(x, y + th, tw, lh))
                elif lh == 0:
                    sub_slots.append(Stitcher.Region(x + tw, y, lw, th))
            for slot in self.sub_slots:
                if slot.add(name, texture): return True
            return False

        def walk(self, counter: Callable[[], None], atlas: Image):
            if self.name is not None:
                atlas.paste(self.texture, (self.x, self.y))
                counter()
            elif self.sub_slots is not None:
                for region in self.sub_slots:
                    region.walk(counter, atlas)

        def find(self, name: str):
            if self.name == name:
                return self
            elif self.sub_slots is not None:
                for region in self.sub_slots:
                    sub = region.find(name)
                    if sub is not None: return sub
            return None

    @staticmethod
    def compare_textures(tuple0: tuple[str, Image], tuple1: tuple[str, Image]) -> int:
        image1 = tuple0[1]
        image2 = tuple1[1]
        compare_a = image1.height - image2.height
        if compare_a != 0: return compare_a
        compare_b = image1.width - image2.width
        if compare_b != 0: return compare_b
        return tuple0[0] >= tuple1[0]

    def add_to_storage(self, name: str, texture: Image) -> bool:
        for region in self.storage:
            if region.add(name, texture): return True
        return self.expand(name, texture)

    def expand(self, name: str, texture: Image) -> bool:
        sw = 2 ** math.ceil(math.log2(self.storage_width)) if self.storage_width != 0 else 0
        sh = 2 ** math.ceil(math.log2(self.storage_height)) if self.storage_height != 0 else 0
        tw = 2 ** math.ceil(math.log2(self.storage_width + texture.width))
        th = 2 ** math.ceil(math.log2(self.storage_height + texture.height))
        if tw > self.max_width and th > self.max_height:
            return False
        expand_width = self.max_width >= tw != sw
        expand_height = self.max_height >= th != sh
        if expand_width ^ expand_height:
            expand = expand_width
        else:
            expand = self.max_width >= tw and sw <= sh
        if expand:
            if self.storage_height == 0:
                self.storage_height = th
            region = Stitcher.Region(self.storage_width, 0, tw - self.storage_width, self.storage_height)
            self.storage_width = tw
        else:
            region = Stitcher.Region(0, self.storage_height, self.storage_width, th - self.storage_height)
            self.storage_height = th
        region.add(name, texture)
        self.storage.append(region)
        return True

    def stitch(self):
        sorted_textures = sorted(
            self.wait_stitched,
            key=cmp_to_key(self.compare_textures),
            reverse=True
        )
        for name, texture in tqdm.tqdm(sorted_textures, desc='Computing region to put sprites'):
            if not self.add_to_storage(name, texture):
                raise BufferError('Storage is too small to contain all sprites')
        pbar = tqdm.tqdm(total=len(sorted_textures), desc='Creating atlas')
        counter = lambda: pbar.update(1)
        atlas = Image.new('RGBA', (self.storage_width, self.storage_height))
        for region in self.storage:
            region.walk(counter, atlas)
        pbar.close()
        return atlas

    def find_region(self, name: str) -> Region | None:
        for region in self.storage:
            sub = region.find(name)
            if sub is not None: return sub
        return None


def load_model_texture(name: str, textures: dict, need_textures: set) -> str:
    if name.startswith('#'):
        if name[1:] in textures and textures[name[1:]] != name:
            return load_model_texture(textures[name[1:]], textures, need_textures)
        else:
            return name
    else:
        if not name.startswith('minecraft:'):
            name = f'minecraft:{name}'
        need_textures.add(name)
        return name


def load_model(name: str, jar: ZipFile, need_textures: set, resolved_models: dict) -> dict:
    if name in resolved_models:
        return resolved_models[name]
    model_path = 'assets/minecraft/models/' + name.replace('minecraft:', '') + '.json'
    model_data: dict = json.loads(jar.read(model_path))
    if 'textures' not in model_data:
        model_data['textures'] = {}
    if 'parent' in model_data:
        parent = load_model(model_data['parent'], jar, need_textures, resolved_models)
        for key in parent:
            if key not in model_data:
                model_data[key] = copy.deepcopy(parent[key])
            if key == 'textures':
                for texture in parent['textures']:
                    if texture not in model_data['textures']:
                        model_data['textures'][texture] = parent['textures'][texture]
    if 'parent' in model_data: model_data.pop('parent')
    if 'display' in model_data: model_data.pop('display')
    if 'gui_light' in model_data: model_data.pop('gui_light')
    if 'ambientocclusion' in model_data: model_data.pop('ambientocclusion')
    for texture in model_data['textures']:
        model_data['textures'][texture] = load_model_texture(
            model_data['textures'][texture], model_data['textures'], need_textures
        )
    resolved_models[name] = model_data
    return model_data


def main_procedure(input_jar: str, output: str):
    if os.path.isdir(output):
        shutil.rmtree(output)
    os.mkdir(output)

    with ZipFile(input_jar, 'r') as jar:
        blockstates = {}
        for file in jar.namelist():
            if file.startswith('assets/minecraft/blockstates/'):
                blockstates[file] = jar.read(file)

        need_models = set()
        for blockstate in tqdm.tqdm(blockstates, desc='Collecting Block States'):
            blockstate_data = json.loads(blockstates[blockstate])
            collected_models = set()
            candidate_models = []
            if 'variants' in blockstate_data:
                for variant in blockstate_data['variants']:
                    candidate_models.append(blockstate_data['variants'][variant])
            elif 'multipart' in blockstate_data:
                for data in blockstate_data['multipart']:
                    candidate_models.append(data['apply'])
            for candidate in candidate_models:
                if 'model' in candidate:
                    collected_models.add(candidate['model'])
                else:
                    for model in candidate:
                        collected_models.add(model['model'])
            need_models.update(collected_models)

        need_textures = set()
        resolved_models = {}
        for model in tqdm.tqdm(need_models, desc='Collecting Models'):
            resolved_models[model] = load_model(model, jar, need_textures, resolved_models)

        for special in SPECIALS:
            for texture in SPECIALS[special]:
                need_textures.add(f'minecraft:{texture}')

        non_stitch_textures = {}
        need_textures.add('minecraft:missingno')
        for texture in tqdm.tqdm(need_textures, desc='Collecting Textures'):
            if texture == 'minecraft:missingno':
                missingno = Image.new('RGBA', (16, 16), 0xFF000000)
                draw = ImageDraw.Draw(missingno)
                draw.rectangle((0, 8, 7, 17), fill=(248, 0, 248))
                draw.rectangle((8, 0, 16, 7), fill=(248, 0, 248))
                non_stitch_textures[texture] = missingno
            else:
                texture_path = 'assets/minecraft/textures/' + texture.replace('minecraft:', '') + '.png'
                non_stitch_textures[texture] = Image.open(io.BytesIO(jar.read(texture_path)))
                if non_stitch_textures[texture] is None:
                    print(f'Warning: {texture} not found!')

        stitcher = Stitcher()
        texture_no_map = {}
        for wait_stitch in tqdm.tqdm(non_stitch_textures, desc='Preparing stitching textures'):
            texture_resolved = stitcher.add_texture(wait_stitch, non_stitch_textures[wait_stitch], jar)
            texture_no_map[wait_stitch] = texture_resolved
            if isinstance(texture_resolved, dict):
                for frame in texture_resolved['frames']:
                    texture_no_map[frame] = frame
        stitcher.stitch().save(f'{output}/atlas.png')

        # Model Indexing
        model_index = {}
        index = 0
        for model in resolved_models:
            model_index[model] = index
            index += 1
        indexed_blockstates = {}
        for blockstate in tqdm.tqdm(blockstates, desc='Indexing Models'):
            blockstate_data = json.loads(blockstates[blockstate])
            candidate_models = []
            if 'variants' in blockstate_data:
                for variant in blockstate_data['variants']:
                    candidate_models.append(blockstate_data['variants'][variant])
            elif 'multipart' in blockstate_data:
                for data in blockstate_data['multipart']:
                    candidate_models.append(data['apply'])
            for candidate in candidate_models:
                if 'model' in candidate:
                    candidate['model'] = model_index[candidate['model']]
                else:
                    for model in candidate:
                        model['model'] = model_index[model['model']]
            indexed_blockstates[blockstate[29:-5]] = blockstate_data
        with open(f'{output}/block_states.json', 'w') as fp:
            json.dump(indexed_blockstates, fp, sort_keys=True, indent='  ')

        # Texture Indexing
        texture_index = {}
        index = 0
        for texture in texture_no_map:
            texture_index[texture] = index
            index += 1
        indexed_models = {}
        for model in tqdm.tqdm(need_models, desc='Indexing Model Textures'):
            resolved_model = resolved_models[model]
            model_inline_textures = {}
            for k in resolved_model['textures']:
                model_inline_textures[k] = texture_index[resolved_model['textures'][k]]
            resolved_model.pop('textures')
            if 'elements' in resolved_model:
                elements = resolved_model['elements']
                for element in elements:
                    if '__comment' in element: element.pop('__comment')
                    for face_id in element['faces']:
                        face = element['faces'][face_id]
                        key = face['texture']
                        face['texture'] = model_inline_textures[key[1:] if key.startswith('#') else key]
            indexed_models[model_index[model]] = resolved_model
        with open(f'{output}/block_models.json', 'w') as fp:
            json.dump(indexed_models, fp, sort_keys=True, indent='  ')

        indexed_textures = {}
        for texture in tqdm.tqdm(texture_no_map, desc='Saving Textures Atlas Data'):
            value = texture_no_map[texture]
            if isinstance(value, str):
                region = stitcher.find_region(texture)
                indexed_textures[texture_index[texture]] = [region.x, region.y, region.w, region.h]
            else:
                value['frames'] = [texture_index[i] for i in value['frames']]
                indexed_textures[texture_index[texture]] = value
        with open(f'{output}/atlas.json', 'w') as fp:
            json.dump(indexed_textures, fp, sort_keys=True, indent='  ')

        indexed_specials = {}
        for special in tqdm.tqdm(SPECIALS, desc='Indexing special textures'):
            indexed_specials[special] = [texture_index[f'minecraft:{t}'] for t in SPECIALS[special]]
        with open(f'{output}/special.json', 'w') as fp:
            json.dump(indexed_specials, fp, sort_keys=True, indent='  ')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('input', help='Minecraft Jar')
    parser.add_argument('--output', help='Output directory', default='./output')
    args = parser.parse_args()
    main_procedure(args.input, args.output)
