package com.ordermgmt.railway.ui.component.pathmanager;

import java.util.UUID;

import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;

/** Sealed hierarchy for heterogeneous TreeGrid nodes in the Path Manager. */
public sealed interface TreeNode {

    String label();

    String type();

    UUID id();

    record YearNode(PmTimetableYear year) implements TreeNode {
        @Override
        public String label() {
            return year.getLabel() != null ? year.getLabel() : "FPJ " + year.getYear();
        }

        @Override
        public String type() {
            return "YEAR";
        }

        @Override
        public UUID id() {
            return year.getId();
        }
    }

    record TrainNode(PmReferenceTrain train) implements TreeNode {
        @Override
        public String label() {
            String otn = train.getOperationalTrainNumber();
            return otn != null ? otn : train.getTridCore();
        }

        @Override
        public String type() {
            return "TRAIN";
        }

        @Override
        public UUID id() {
            return train.getId();
        }
    }

    record VersionNode(PmTrainVersion version) implements TreeNode {
        @Override
        public String label() {
            return version.getLabel() != null
                    ? version.getLabel()
                    : "v" + version.getVersionNumber();
        }

        @Override
        public String type() {
            return "VERSION";
        }

        @Override
        public UUID id() {
            return version.getId();
        }
    }

    record LocationNode(PmJourneyLocation location) implements TreeNode {
        @Override
        public String label() {
            String name = location.getPrimaryLocationName();
            return name != null ? name : "Seq " + location.getSequence();
        }

        @Override
        public String type() {
            return "OP";
        }

        @Override
        public UUID id() {
            return location.getId();
        }
    }
}
