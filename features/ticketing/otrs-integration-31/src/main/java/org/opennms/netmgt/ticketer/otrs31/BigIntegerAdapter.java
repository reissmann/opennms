/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2013 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2013 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.ticketer.otrs31;

import java.math.BigInteger;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class BigIntegerAdapter extends XmlAdapter<String, BigInteger> {

    @Override
    public String marshal(BigInteger value) {
        if (value == null) {
            return null;
        }
        return (javax.xml.bind.DatatypeConverter.printInteger(value));
    }

    @Override
    public BigInteger unmarshal(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        } else {
            return (javax.xml.bind.DatatypeConverter.parseInteger(value));
        }

    }

}
