package com.helpdesk.itagent.rag;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class EmbeddingServiceTest {

    @Test
    void recognizesVpnParaphrasesAsRelated() {
        String vpnGuide = "Cisco Secure Client VPN gateway and MFA connection steps";

        assertThat(EmbeddingService.semanticSimilarity("My VPN is not working", vpnGuide))
                .isGreaterThan(0.32);
        assertThat(EmbeddingService.semanticSimilarity("I cannot connect remotely to company systems", vpnGuide))
                .isGreaterThan(0.32);
        assertThat(EmbeddingService.semanticSimilarity("The AnyConnect tunnel keeps failing", vpnGuide))
                .isGreaterThan(0.32);
    }

    @Test
    void keepsUnrelatedQuestionsSeparate() {
        String vpnGuide = "Cisco Secure Client VPN gateway and MFA connection steps";

        assertThat(EmbeddingService.semanticSimilarity("How do I deploy a payroll microservice?", vpnGuide))
                .isLessThan(0.32);
    }
}