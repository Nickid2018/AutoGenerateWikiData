package net.minecraft.core;

import io.github.nickid2018.util.SneakyUtil;
import net.minecraft.tags.TagKey;

import java.util.stream.Stream;

public interface HolderSet<T> {

    Stream<Holder<T>> stream();

    abstract class ListBacked<T> implements HolderSet<T> {
        public Stream<Holder<T>> stream() {
            return SneakyUtil.sneakyNotNull();
        }
    }

    class Named<T> extends ListBacked<T> {
        public TagKey<T> key() {
            return SneakyUtil.sneakyNotNull();
        }
    }
}
