package com.onetuks.iflow_sentinel.parser.model;

import java.util.List;

/** .iflw(BPMN2 XML) 전체를 파싱한 결과. */
public record IflowModel(
        IflowConfig config,
        List<Participant> participants,
        List<Channel> channels,
        List<ProcessNode> processes,
        List<EventNode> events,
        List<GatewayNode> gateways,
        List<SequenceFlow> sequenceFlows,
        List<StepNode> steps,
        List<ExceptionSubprocess> exceptionSubprocesses
) {
    public IflowModel {
        participants = List.copyOf(participants);
        channels = List.copyOf(channels);
        processes = List.copyOf(processes);
        events = List.copyOf(events);
        gateways = List.copyOf(gateways);
        sequenceFlows = List.copyOf(sequenceFlows);
        steps = List.copyOf(steps);
        exceptionSubprocesses = List.copyOf(exceptionSubprocesses);
    }
}
