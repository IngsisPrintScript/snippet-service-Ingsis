package com.ingsis.snippet_service.snippet.dto;

import java.util.UUID;

public class TestReceiveDTO {

        final String name;
        final String input;
        final String output;

        public TestReceiveDTO(String name, String input, String output) {
            this.name = name;
            this.input = input;
            this.output = output;
        }

        public String getName() {
            return name;
        }

        public String getInput() {
            return input;
        }

        public String getOutput() {
            return output;
        }

}
