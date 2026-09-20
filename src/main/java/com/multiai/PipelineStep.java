package com.multiai;

import com.multiai.adapters.AIAdapter;

import java.util.List;

/**
 * PipelineStep
 * ------------
 * One stage in the multi-AI pipeline: which adapter runs, what task it
 * performs, which tags of prior context it should read, and which tag(s)
 * its own output should be filed under. Adding a third, fourth, or fifth
 * AI to the pipeline (e.g. Coding -> Testing -> Documentation, as in the
 * original proposal) just means adding another PipelineStep to the list
 * in Main.java — nothing else needs to change.
 */
public class PipelineStep {

    public final AIAdapter adapter;
    public final String task;
    public final List<String> readTags;
    public final List<String> writeTags;
    public final String outputRole;

    public PipelineStep(AIAdapter adapter, String task, List<String> readTags,
                         List<String> writeTags, String outputRole) {
        this.adapter = adapter;
        this.task = task;
        this.readTags = readTags;
        this.writeTags = writeTags;
        this.outputRole = outputRole;
    }
}
