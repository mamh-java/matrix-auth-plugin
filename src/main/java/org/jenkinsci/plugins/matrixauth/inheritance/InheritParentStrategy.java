/*
 * The MIT License
 *
 * Copyright (c) 2017 Daniel Beck
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.matrixauth.inheritance;

import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.security.Permission;
import org.acegisecurity.Authentication;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Strategy that inherits the ACL from the parent.
 *
 * The paren't inheritance strategy in turn determines whether this receives permissions from grandparents etc. up to root.
 */
public class InheritParentStrategy extends InheritanceStrategy {

    @DataBoundConstructor
    public InheritParentStrategy() {

    }

    @Override
    protected boolean hasPermission(@Nonnull Authentication a, @Nonnull Permission permission, ACL child, @CheckForNull ACL parent, ACL root) {
        if (a.equals(ACL.SYSTEM)) {
            return true;
        }
        if (isParentReadPermissionRequired() && parent != null && (Item.READ.equals(permission) || Item.DISCOVER.equals(permission))) {
            /*
             * If we have an item parent, only grant Item/Read and Item/Discover if it's granted on the parent.
             * In this case, it doesn't even matter whether it's explicitly granted on the child.
             */
            return parent.hasPermission(a, permission);
        }
        if (parent == null) {
            /*
             * Without an item parent (i.e. topmost level item) we need to check both grants on this item, as
             * well as grants on the root (parent) ACL:
             * - Explicitly granted here but possibly not globally (on root): That's OK
             * - NOT explicitly granted here, but globally: That's also OK
             */
            return root.hasPermission(a, permission) || child.hasPermission(a, permission);
        } else {
            /* If we have an item parent, check both explicit grants here and inherited permissions from parent. */
            return parent.hasPermission(a, permission) || child.hasPermission(a, permission);
        }
    }

    @Symbol("inheriting")
    @Extension(ordinal = 100)
    public static class DescriptorImpl extends InheritanceStrategyDescriptor {

        @Override
        public boolean isApplicable(Class<?> clazz) {
            return AbstractItem.class.isAssignableFrom(clazz);
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.InheritParentStrategy_DisplayName();
        }
    }
}
