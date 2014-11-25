/*
 * Copyright (C) 2007, 2014 Graham Sanderson
 *
 * This file is part of JPSX.
 * 
 * JPSX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPSX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JPSX.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpsx.api.components.core;

public class ContinueExecutionException extends RuntimeException {
    private boolean skipCurrentInstruction;

    public static ContinueExecutionException SKIP_CURRENT = new ContinueExecutionException(true);
    public static ContinueExecutionException DONT_SKIP_CURRENT = new ContinueExecutionException(false);

    public ContinueExecutionException() {
        this(false);
    }

    public ContinueExecutionException(boolean skipCurrentInstruction) {
        this.skipCurrentInstruction = skipCurrentInstruction;
    }

    public boolean skipCurrentInstruction() {
        return skipCurrentInstruction;
    }
}
