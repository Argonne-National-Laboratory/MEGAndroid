/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This software has been modified by Greg Rehm 3/9/16
 */

package gov.anl.coar.meg.exception;

public class PgpKeyNotFoundException extends Exception {
    static final long serialVersionUID = 0xf812773342L;

    public PgpKeyNotFoundException(String message) {
        super(message);
    }
    public PgpKeyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public PgpKeyNotFoundException(Throwable cause) {
        super(cause);
    }
}
