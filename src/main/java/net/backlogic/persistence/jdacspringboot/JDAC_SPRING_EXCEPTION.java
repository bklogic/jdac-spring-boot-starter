package net.backlogic.persistence.jdacspringboot;

/**
 * JDAC exception. Thrown when exception condition detected by JDAC starter.
 */
public class JDAC_SPRING_EXCEPTION extends RuntimeException {
    public JDAC_SPRING_EXCEPTION(String message) {
        super(message);
    }

}
