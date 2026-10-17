package sn.uasz.voteuasz.elections;

public enum ElectionStatus {
    DRAFT,
    CANDIDATURES_OPEN,
    CANDIDATURES_CLOSED,
    VALIDATION,
    READY_TO_VOTE,
    VOTING_OPEN,
    VOTING_CLOSED,
    COUNTING,
    RESULTS_PUBLISHED,
    ARCHIVED;

    public boolean canTransitionTo(ElectionStatus next) {
        return switch (this) {
            case DRAFT -> next == CANDIDATURES_OPEN || next == READY_TO_VOTE;
            case CANDIDATURES_OPEN -> next == CANDIDATURES_CLOSED;
            case CANDIDATURES_CLOSED -> next == VALIDATION;
            case VALIDATION -> next == READY_TO_VOTE || next == CANDIDATURES_OPEN;
            case READY_TO_VOTE -> next == VOTING_OPEN;
            case VOTING_OPEN -> next == VOTING_CLOSED;
            case VOTING_CLOSED -> next == COUNTING;
            case COUNTING -> next == RESULTS_PUBLISHED;
            case RESULTS_PUBLISHED -> next == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
