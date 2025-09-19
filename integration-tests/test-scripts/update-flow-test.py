#!/usr/bin/env python3
"""
End-to-end integration test for WCFC Groups update flow
Includes WireMock setup and complete user workflow testing
"""

import json
import os
import re
import sys
import time
import requests
from playwright.sync_api import sync_playwright, expect

def setup_wiremock_stubs():
    """Setup WireMock stubs for Groups.io and WCFC-Manuals APIs"""
    print("Setting up WireMock stubs for Groups.io and WCFC-Manuals APIs...")
    
    wiremock_url = "http://localhost:8080"
    
    # Wait for WireMock to be ready
    for i in range(30):
        try:
            response = requests.get(f"{wiremock_url}/__admin/health")
            if response.status_code == 200:
                break
        except requests.exceptions.ConnectionError:
            pass
        time.sleep(1)
    else:
        raise Exception("WireMock is not ready")
    
    # Clear existing stubs
    requests.delete(f"{wiremock_url}/__admin/mappings")
    
    # Groups.io API - Add member endpoint stub
    groupsio_add_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/api/v1/directadd",
            "headers": {
                "Authorization": {
                    "matches": "Bearer .*"
                },
                "Content-Type": {
                    "equalTo": "application/x-www-form-urlencoded"
                }
            }
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=groupsio_add_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create Groups.io add member stub: {response.text}")
    
    print("Created Groups.io add member stub")
    
    # Groups.io API - Remove member endpoint stub
    groupsio_remove_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/api/v1/bulkremovemembers",
            "headers": {
                "Authorization": {
                    "matches": "Bearer .*"
                },
                "Content-Type": {
                    "equalTo": "application/x-www-form-urlencoded"
                }
            }
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=groupsio_remove_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create Groups.io remove member stub: {response.text}")
    
    print("Created Groups.io remove member stub")
    
    # WCFC-Manuals API - Add member endpoint stub
    manuals_add_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/api/member/add",
            "headers": {
                "X-WCFC-TOKEN": {
                    "matches": ".*"
                }
            }
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=manuals_add_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create WCFC-Manuals add member stub: {response.text}")
    
    print("Created WCFC-Manuals add member stub")
    
    # WCFC-Manuals API - Remove member endpoint stub
    manuals_remove_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/api/member/remove",
            "headers": {
                "X-WCFC-TOKEN": {
                    "matches": ".*"
                }
            }
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=manuals_remove_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create WCFC-Manuals remove member stub: {response.text}")
    
    print("Created WCFC-Manuals remove member stub")
    
    print("WireMock stubs setup completed successfully!")

def get_api_requests():
    """Get all API requests sent to WireMock"""
    wiremock_url = "http://localhost:8080"
    
    response = requests.get(f"{wiremock_url}/__admin/requests")
    if response.status_code == 200:
        requests_data = response.json()
        api_requests = {
            'groupsio_add': [],
            'groupsio_remove': [],
            'manuals_add': [],
            'manuals_remove': []
        }
        
        all_requests = requests_data.get('requests', [])
        
        for request in all_requests:
            # The URL is nested inside request.request.url
            request_data = request.get('request', {})
            url = request_data.get('url', '')
            if '/api/v1/directadd' in url:
                api_requests['groupsio_add'].append(request)
            elif '/api/v1/bulkremovemembers' in url:
                api_requests['groupsio_remove'].append(request)
            elif '/api/member/add' in url:
                api_requests['manuals_add'].append(request)
            elif '/api/member/remove' in url:
                api_requests['manuals_remove'].append(request)
        
        return api_requests
    else:
        raise Exception(f"Failed to get requests from WireMock: {response.text}")

def wait_for_page_ready(page, max_attempts=5):
    """Wait for page to be fully loaded and JavaScript ready"""
    print("Waiting for page to be ready...")
    
    for attempt in range(max_attempts):
        page.wait_for_load_state("load")
        page.wait_for_timeout(2000)  # Wait for JS to initialize
        
        # Check if we have actual content (not just modulepreload)
        content = page.content()
        if "modulepreload" in content and len(content) < 2000:
            print(f"Attempt {attempt + 1}: Page not fully loaded, waiting...")
            page.wait_for_timeout(3000)
            
            # Try to trigger loading
            try:
                page.evaluate("window.scrollTo(0, 100)")
                page.wait_for_timeout(1000)
            except:
                pass
        else:
            print("✓ Page is ready")
            return True
    
    # Last resort: hard refresh
    print("Page still not ready, trying hard refresh...")
    page.reload(wait_until="load")
    page.wait_for_timeout(5000)
    return True

def run_update_test():
    """Run the complete update flow test"""
    print("Starting update flow test...")
    
    # Setup WireMock stubs first
    try:
        setup_wiremock_stubs()
        print("✅ WireMock stubs setup completed")
    except Exception as e:
        print(f"❌ Failed to setup WireMock stubs: {e}")
        return False
    
    with sync_playwright() as p:
        # Launch browser
        browser = p.chromium.launch(
            headless=True,
            args=[
                '--no-sandbox',
                '--disable-dev-shm-usage',
                '--disable-web-security',
                '--allow-running-insecure-content'
            ]
        )
        
        context = browser.new_context(
            viewport={'width': 1280, 'height': 720},
            java_script_enabled=True,
            ignore_https_errors=True
        )
        
        page = context.new_page()
        
        # Simple console logging
        page.on("console", lambda msg: print(f"Console: {msg.text}") if "404" not in msg.text else None)
        page.on("pageerror", lambda error: print(f"Page error: {error}"))
        
        try:
            # Step 1: Navigate to main page
            print("Step 1: Navigating to /...")
            
            response = page.goto("http://localhost:9301/", wait_until="load", timeout=15000)
            print(f"Navigation response: {response.status}")
           
            # Wait for page to be ready
            wait_for_page_ready(page)

            # Step 2: Upload the sample xls file from test-data
            print("Step 2: Uploading XLS file...")
            
            # Look for file upload input
            file_input = page.locator('input[type="file"]')
            expect(file_input).to_be_visible(timeout=10000)
            
            # Upload the test file
            test_file_path = "/app/test-data/myfbo-report.xls"
            file_input.set_input_files(test_file_path)
            print(f"✅ File uploaded: {test_file_path}")

            # Step 3: View the checkbox page and see that there is one addition and one deletion
            print("Step 3: Waiting for results page with checkboxes...")
            
            # Wait for the page to process and show results
            wait_for_page_ready(page)
            
            # Look for checkboxes - should be one for additions and one for removals
            add_checkbox = page.locator('input[type="checkbox"]').filter(has_text=re.compile(r'add|addition', re.IGNORECASE)).first
            remove_checkbox = page.locator('input[type="checkbox"]').filter(has_text=re.compile(r'remove|removal|delete', re.IGNORECASE)).first
            
            # Alternative: look for any checkboxes if the above doesn't work
            if not add_checkbox.is_visible() or not remove_checkbox.is_visible():
                checkboxes = page.locator('input[type="checkbox"]')
                checkbox_count = checkboxes.count()
                print(f"Found {checkbox_count} checkboxes")
                
                if checkbox_count == 2:
                    add_checkbox = checkboxes.nth(0)
                    remove_checkbox = checkboxes.nth(1)
                else:
                    raise Exception(f"Expected 2 checkboxes (add and remove), but found {checkbox_count}")
            
            expect(add_checkbox).to_be_visible(timeout=10000)
            expect(remove_checkbox).to_be_visible(timeout=10000)
            print("✅ Found add and remove checkboxes")

            # Step 4: Check both boxes and submit
            print("Step 4: Checking both boxes and submitting...")
            
            add_checkbox.check()
            remove_checkbox.check()
            print("✅ Both checkboxes checked")
            
            # Look for submit button
            submit_button = page.locator('button:has-text("Submit"), button:has-text("Apply"), input[type="submit"]').first
            expect(submit_button).to_be_visible(timeout=5000)
            submit_button.click()
            print("✅ Submit button clicked")
            
            # Wait for processing to complete
            wait_for_page_ready(page)

            # Step 5: Verify that WireMock has received updates for both groups.io and wcfc-manuals
            print("Step 5: Verifying API calls to WireMock...")
            
            # Wait a bit for all API calls to complete
            time.sleep(3)
            
            # Get all API requests from WireMock
            api_requests = get_api_requests()
            
            # Verify Groups.io API calls
            groupsio_add_calls = len(api_requests['groupsio_add'])
            groupsio_remove_calls = len(api_requests['groupsio_remove'])
            
            # Verify WCFC-Manuals API calls  
            manuals_add_calls = len(api_requests['manuals_add'])
            manuals_remove_calls = len(api_requests['manuals_remove'])
            
            print(f"API call summary:")
            print(f"  Groups.io add calls: {groupsio_add_calls}")
            print(f"  Groups.io remove calls: {groupsio_remove_calls}")
            print(f"  WCFC-Manuals add calls: {manuals_add_calls}")
            print(f"  WCFC-Manuals remove calls: {manuals_remove_calls}")

            # Verify we have the expected API calls
            if groupsio_add_calls != 1:
                raise Exception("Expected exactly 1 groups.io add call")
            if groupsio_remove_calls != 1:
                raise Exception("Expected exactly 1 groups.io remove call")
            if manuals_add_calls != 1:
                raise Exception("Expected exactly 1 wcfc-manuals add call")
            if manuals_remove_calls != 1:
                raise Exception("Expected exactly 1 wcfc-manuals remove call")
            
            print("✅ Update flow completed successfully!")
            return True
            
        except Exception as e:
            print(f"❌ Test failed: {e}")
            
            # Take screenshot for debugging
            try:
                page.screenshot(path="/app/output/test-failure.png")
                print("Screenshot saved to output/test-failure.png")
            except:
                pass
            
            # Print page content for debugging
            try:
                content = page.content()
                print(f"Page content length: {len(content)}")
                if len(content) < 5000:  # Only print if not too long
                    print("Page content:")
                    print(content[:2000] + "..." if len(content) > 2000 else content)
            except:
                pass

            return False
 
        finally:
            browser.close()

if __name__ == "__main__":
    try:
        success = run_update_test()
        if success:
            print("\n✅ Integration test PASSED!")
            sys.exit(0)
        else:
            print("\n❌ Integration test FAILED!")
            sys.exit(1)
    except Exception as e:
        print(f"\n💥 Test execution error: {e}")
        sys.exit(1)
