package vn.edu.ptit.holidayplanner.media;

public enum ImageUploadKind {
    AVATAR("avatars", "c_fill,g_face,h_512,w_512/q_auto"),
    DESTINATION("destinations", "c_limit,h_1080,w_1920/q_auto");

    private final String folder;
    private final String transformation;

    ImageUploadKind(String folder, String transformation) {
        this.folder = folder;
        this.transformation = transformation;
    }

    public String folder() {
        return folder;
    }

    public String transformation() {
        return transformation;
    }
}
