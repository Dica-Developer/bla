/*******************************************************************************
 * Copyright (c) MOBAC developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.utilities.file;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

public class RegexFileFilter implements FileFilter {

	private Pattern p;

	public RegexFileFilter(String regex) {
		p = Pattern.compile(regex);
	}

	public boolean accept(File pathname) {
		return p.matcher(pathname.getName()).matches();
	}

}
