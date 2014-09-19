package com.codexperiments.leakeeper.config.resolver;

public interface EmitterResolver {
    /**
     * Gives an object identifying an emitter. Basicallt, this identifier is used later as an indirection to access the emitter
     * (actually a Weak reference to it) while avoiding any possible memory leak. This Id could be a String, an Integer
     * constant... Anything that identifies the emitter uniquely. For example:
     * <ul>
     * <li>if an Activity, like a Dashboard activity, is unique in the app, then we can use its class as an Id (i.e. one instance
     * at once, although activity can be recreated. But any task that is emitted by a HomeActivity can be bound to any later
     * instance of a HomeActivity).</li>
     * <li>If an activity, let's imagine a Web activity displaying a single specific web page, is used several times in the same
     * app, then it may be appropriate to use a more specific Id, such as page Url (e.g. for a task that loads the corresponding
     * page). We don't want an Activity to display the result of another.</li>
     * <ul>
     *
     * @param pEmitter Emitter of a task the Id of which is needed.
     * @return Id of the emitter. Cannot be the emitter itself, or that could potentienally result in a memory leak.
     */
    Object resolveEmitterId(Object pEmitter);
}
