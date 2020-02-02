package jackplay.core;

import jackplay.JackplayLogger;
import jackplay.model.Category;
import jackplay.model.Options;
import jackplay.model.Site;
import static jackplay.model.Category.*;

import jackplay.core.performers.RedefinePerformer;
import jackplay.core.performers.TracingPerformer;
import jackplay.core.performers.Performer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <code>Registry</code> manages tracing requests.
 */
public class Registry {
    private Map<Category, Map<String, Map<String, Performer>>> registry;

    public Registry(Options options) {
        this.registry = new ConcurrentHashMap<>();
        this.prepareGenre(TRACE);
        this.prepareGenre(REDEFINE);
        this.addTraces(options.defaultTraceAsArray());
    }

    public synchronized boolean register(Category category, Site site, String newBody) {
        if (TRACE == category && this.wasRegistered(TRACE, site)) {
            return false;
        }

        prepareClass(category, site.classFullName);
        Performer performer = createPerformer(site, category, newBody);

        // todo, use method shortname + argslist instead of method full name
        registry.get(category).get(site.classFullName).put(site.methodFullName, performer);

        JackplayLogger.info("registry", "has registered:" + category + ", " + site.methodFullName);
        return true;
    }

    public synchronized boolean unregister(Category category, Site site) {
        if (!this.wasRegistered(category, site)) {
            return false;
        }

        if (registry.get(category).containsKey(site.classFullName)) {

            registry.get(category).get(site.classFullName).remove(site.methodFullName);
            JackplayLogger.info("program-manager", "deleted existing agenda:" + category + ", " + site.methodFullName);

            if (registry.get(category).get(site.classFullName).isEmpty()) {
                registry.get(category).remove(site.classFullName);
            }
        }

        return true;
    }

    private boolean wasRegistered(Category category, Site pg) {
        try {
            return registry.get(category).get(pg.classFullName).containsKey(pg.methodFullName);
        } catch(NullPointerException npe) {
            return false;
        }
    }

    private void prepareGenre(Category category) {
        if (!registry.containsKey(category)) {
            registry.put(category, new ConcurrentHashMap<>());
        }
    }

    private void prepareClass(Category category, String className) {
        if (!registry.get(category).containsKey(className)) registry.get(category).put(className, new ConcurrentHashMap<String, jackplay.core.performers.Performer>());
    }

    private Performer createPerformer(Site pg, Category category, String methodSource) {
        switch (category) {
            case TRACE:
                return new TracingPerformer(pg);
            case REDEFINE:
                return new RedefinePerformer(pg, methodSource);
            default:
                throw new RuntimeException("unknown genre " + category.toString());
        }
    }

    public synchronized Map<Category, Map<String, Performer>> agendaForClass(String classFullName) {
        Map<Category, Map<String, Performer>> agenda = new HashMap<>();
        agenda.put(TRACE, this.registry.get(TRACE).get(classFullName));
        agenda.put(REDEFINE, this.registry.get(REDEFINE).get(classFullName));

        return agenda;
    }

    public synchronized void addTraces(String[] methodFullNames) {
        if (methodFullNames == null || methodFullNames.length == 0) return;

        for (String mfn : methodFullNames) {
            if (mfn == null) continue;

            String trimmed = mfn.trim();
            if (trimmed.length() == 0) continue;

            this.register(TRACE, new Site(mfn), null);
        }
    }

    synchronized Performer existingPerformer(Category category, String classFullName, String methodFullName) {
        try {
            return registry.get(category).get(classFullName).get(methodFullName);
        } catch (NullPointerException npe) {
            return null;
        }
    }

    synchronized Map<String, ?> agendaOfGenre(Category category) {
        return this.registry.get(category);
    }

    synchronized Map<Category, Map<String, Map<String, Performer>>> copyOfCurrentProgram() {
        Map<Category, Map<String, Map<String, Performer>>> copy = new HashMap<>();
        deepMapCopy(registry, copy);

        return copy;
    }

    @SuppressWarnings("unchecked")
    private void deepMapCopy(Map source, Map target) {
        for (Object key : source.keySet()) {
            Object value = source.get(key);
            if (value instanceof Map) {
                Map valueCopy = new HashMap();
                deepMapCopy((Map) value, valueCopy);

                target.put(key, valueCopy);
            } else {
                target.put(key, value);
            }
        }
    }
}
