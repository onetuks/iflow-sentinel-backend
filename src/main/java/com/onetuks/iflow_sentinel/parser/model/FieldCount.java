package com.onetuks.iflow_sentinel.parser.model;

/** .mmap의 &lt;brick type="Src"/"Dst"&gt; 개수 집계. */
public record FieldCount(int source, int target) {
}
