package net.minecraft.client.renderer.chunk;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class SectionRenderDispatcher {
    public AtomicInteger toBatchCount;
    public Queue<Object> toBatchHighPriority;
    public Queue<Object> toBatch;

    public boolean runTask() { return false; }

    public static class RenderSection {
        public CompiledSection compiled;
        public boolean dirty;

        public Object getOrigin() { return null; }
    }

    public static class CompiledSection {
        public Set<Object> hasBlocks;
        public Object visibilitySet;
    }
}
