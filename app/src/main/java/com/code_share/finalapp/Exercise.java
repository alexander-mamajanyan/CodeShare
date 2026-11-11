package com.code_share.finalapp;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Exercise implements Parcelable {
    private String id;           // Exercise ID
    private String title;
    private String description;
    private String difficulty;
    private String category;
    private String code;
    private List<TestCase> testCases;  // New field for test cases
    private List<String> hints;
    private String solutionMethodName;
    private int score;
    private String creatorId;    // User ID of the creator
    private String creatorName;  // Username of the creator
    private boolean approved;    // Whether this exercise is approved by an admin
    private long createdAt;      // Timestamp when exercise was created

    // Inner class to represent a test case
    public static class TestCase implements Parcelable {
        private List<String> inputs;  // List of inputs for the test case
        private String expectedOutput;  // Expected output for these inputs

        public TestCase() {
            this.inputs = new ArrayList<>();
        }

        public TestCase(List<String> inputs, String expectedOutput) {
            this.inputs = new ArrayList<>(inputs);  // Create a new ArrayList to avoid fixed-size list issues
            this.expectedOutput = expectedOutput;
        }

        protected TestCase(Parcel in) {
            inputs = new ArrayList<>();  // Create a new modifiable ArrayList
            in.readStringList(inputs);   // Read into the modifiable list
            expectedOutput = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringList(new ArrayList<>(inputs));  // Write a modifiable copy
            dest.writeString(expectedOutput);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<TestCase> CREATOR = new Creator<TestCase>() {
            @Override
            public TestCase createFromParcel(Parcel in) {
                return new TestCase(in);
            }

            @Override
            public TestCase[] newArray(int size) {
                return new TestCase[size];
            }
        };

        public List<String> getInputs() {
            return new ArrayList<>(inputs);  // Return a copy to ensure modifiability
        }

        public void setInputs(List<String> inputs) {
            this.inputs = inputs != null ? new ArrayList<>(inputs) : new ArrayList<>();
        }

        public String getExpectedOutput() {
            return expectedOutput;
        }

        public void setExpectedOutput(String expectedOutput) {
            this.expectedOutput = expectedOutput;
        }
    }

    // Required empty constructor for Firebase
    public Exercise() {
        this.hints = new ArrayList<>();
        this.testCases = new ArrayList<>();
        this.approved = false;
        this.createdAt = System.currentTimeMillis();
    }

    public Exercise(String id, String title, String description, String difficulty, String category, 
                   String code, List<TestCase> testCases, List<String> hints, 
                   String solutionMethodName, int score, String creatorId, String creatorName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.category = category;
        this.code = code;
        this.testCases = testCases != null ? testCases : new ArrayList<>();
        this.hints = hints != null ? hints : new ArrayList<>();
        this.solutionMethodName = solutionMethodName;
        this.score = score;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.approved = false; // Default to not approved
        this.createdAt = System.currentTimeMillis();
    }

    protected Exercise(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        difficulty = in.readString();
        category = in.readString();
        code = in.readString();
        testCases = new ArrayList<>();
        in.readTypedList(testCases, TestCase.CREATOR);
        hints = new ArrayList<>();
        in.readStringList(hints);
        solutionMethodName = in.readString();
        score = in.readInt();
        creatorId = in.readString();
        creatorName = in.readString();
        approved = in.readByte() != 0;
        createdAt = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(difficulty);
        dest.writeString(category);
        dest.writeString(code);
        dest.writeTypedList(testCases);
        dest.writeStringList(hints);
        dest.writeString(solutionMethodName);
        dest.writeInt(score);
        dest.writeString(creatorId);
        dest.writeString(creatorName);
        dest.writeByte((byte) (approved ? 1 : 0));
        dest.writeLong(createdAt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Exercise> CREATOR = new Creator<Exercise>() {
        @Override
        public Exercise createFromParcel(Parcel in) {
            return new Exercise(in);
        }

        @Override
        public Exercise[] newArray(int size) {
            return new Exercise[size];
        }
    };

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getCategory() {
        return category;
    }

    public String getCode() {
        return code;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public List<String> getHints() {
        return hints;
    }

    public String getSolutionMethodName() {
        return solutionMethodName;
    }

    public int getScore() {
        return score;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases != null ? testCases : new ArrayList<>();
    }

    public void setHints(List<String> hints) {
        this.hints = hints != null ? hints : new ArrayList<>();
    }

    public void setSolutionMethodName(String solutionMethodName) {
        this.solutionMethodName = solutionMethodName;
    }

    public void setScore(int score) {
        this.score = score;
    }
    
    public String getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
    
    public String getCreatorName() {
        return creatorName;
    }
    
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    
    public boolean isApproved() {
        return approved;
    }
    
    public void setApproved(boolean approved) {
        this.approved = approved;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Exercise{" +
                "title='" + title + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", category='" + category + '\'' +
                ", score=" + score +
                '}';
    }
}





