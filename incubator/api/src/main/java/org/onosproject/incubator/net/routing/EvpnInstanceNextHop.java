/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.incubator.net.routing;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * Represents a route.
 */
public final class EvpnInstanceNextHop implements NextHop {

    private final IpAddress nextHop;
    private final Label label;

    // new add
    private EvpnInstanceNextHop(IpAddress nextHop, Label label) {
        this.nextHop = nextHop;
        this.label = label;
    }

    public static EvpnInstanceNextHop evpnNextHop(IpAddress nextHop,
                                                  Label label) {
        return new EvpnInstanceNextHop(nextHop, label);
    }

    /**
     * Returns the next hop IP address.
     *
     * @return next hop
     */
    public IpAddress nextHop() {
        return nextHop;
    }

    public Label label() {
        return label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextHop, label);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnInstanceNextHop)) {
            return false;
        }

        EvpnInstanceNextHop that = (EvpnInstanceNextHop) other;

        return Objects.equals(this.nextHop(), that.nextHop())
                && Objects.equals(this.label, that.label);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("nextHop", this.nextHop())
                .add("label", this.label).toString();
    }
}
