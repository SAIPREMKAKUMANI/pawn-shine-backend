package com.project.pawn.customeronboarding.view;

public class View {
    public interface CustomerBase {}

    public interface ContactInfo extends CustomerBase {}
    public interface AddressInfo extends CustomerBase {}
    public interface IdProofInfo extends CustomerBase {}
    public interface RelativeInfo extends CustomerBase {}

    public interface CustomerDetails extends ContactInfo, AddressInfo, IdProofInfo, RelativeInfo {}
}
