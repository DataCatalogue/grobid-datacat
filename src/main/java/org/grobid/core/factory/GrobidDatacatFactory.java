package org.grobid.core.factory;

import org.grobid.core.engines.EngineDatacat;

/**
 *
 * Factory to get engine instances.
 *
 */
public class GrobidDatacatFactory extends GrobidFactory {

    private static EngineDatacat engine;

    /**
     * The instance of GrobidDatacatFactory.
     */
    private static GrobidDatacatFactory factory = null;

    /**
     * Constructor.
     */
    protected GrobidDatacatFactory() {
        init();
    }

    /**
     * Return a new instance of GrobidDatacatFactory if it doesn't exist, the existing
     * instance else.
     *
     * @return GrobidFactory
     */
    public static GrobidDatacatFactory getInstance() {
        if (factory == null) {
            factory = newInstance();
        }
        return factory;
    }

    public EngineDatacat getEngine() {
        return getEngine(false);
    }

    public  EngineDatacat getEngine(boolean preload) {
        if (engine == null) {
            engine = createEngine(preload);
        }
        return engine;
    }

    public EngineDatacat createEngine() {
        return createEngine(false);
    }

    /**
     * Return a new instance of engine.
     *
     * @return EngineDatacat
     */
    public EngineDatacat createEngine(boolean preload) {
        return new EngineDatacat(preload);
    }

    /**
     * Creates a new instance of GrobidDatacatFactory.
     *
     * @return GrobidDatacatFactory
     */
    protected static GrobidDatacatFactory newInstance() {
        return new GrobidDatacatFactory();
    }

    /**
     * Resets this class and all its static fields. For instance sets the
     * current object to null.
     */
    public static void reset() {
        factory = null;
    }

}
