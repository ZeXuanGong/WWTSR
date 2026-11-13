package com.wwtsr.panel;

import javafx.scene.Node;

/** One page of the panel. */
interface Page {

    /** What to show. */
    Node node();

    /** The player's things changed: show them as they are now. */
    void refresh();
}
