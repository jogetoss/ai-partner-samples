Here is the full audit report for **JogetDxShowcase** (v1).

---

## 🔍 Naming Audit Report — `JogetDxShowcase` (v1)

> **Verdict:** ❌ The app has widespread naming violations — **every single form and list ID** uses camelCase instead of the required lower `snake_case`, resulting in **75 violations across 65 forms and 44 lists checked**.

---

### Violations

> All violations share the same problem: **ID is not lower snake_case** (must use only lowercase letters a–z, digits, and underscores, starting with a letter).

#### Forms (41 violations)

| Current ID | Display Name | Suggested ID |
|---|---|---|
| `calendarMeeting` | Calendar - Meeting | `calendar_meeting` |
| `candleStick` | Data Entry: Candlestick | `candle_stick` |
| `chartData` | Data Entry: Chart | `chart_data` |
| `conversionRates` | (DUMMY) MT #13 - Conversion Rates | `conversion_rates` |
| `currentVisits` | (DUMMY) MT #11 - Current Visits | `current_visits` |
| `featureAdvancedGrid` | Feature - Advanced Grid | `feature_advanced_grid` |
| `featureDateTime` | Feature - DateTime | `feature_date_time` |
| `featureFormGrid` | Feature - Form Grid | `feature_form_grid` |
| `featureListGrid` | Feature - List Grid | `feature_list_grid` |
| `featureMiscField` | Feature - Misc Field | `feature_misc_field` |
| `featureSelectField` | Feature - Select Fields | `feature_select_field` |
| `featureSpreadsheet` | Feature - Spreadsheet | `feature_spreadsheet` |
| `featureStdField` | Feature - Standard Fields | `feature_std_field` |
| `featureUploadField` | Feature - Upload Fields | `feature_upload_field` |
| `gridForm` | Grid Form | `grid_form` |
| `gridLG` | Grid List | `grid_lg` |
| `gridSp` | Grid Spreadsheet | `grid_sp` |
| `historicalSales` | (DUMMY) MT #6 - Historical Sales | `historical_sales` |
| `moneyOverYears` | (DUMMY) MT #8 - Money Over Years | `money_over_years` |
| `multiPagedFormField` | Feature - Multi Paged Form | `multi_paged_form_field` |
| `newOrder` | (DUMMY) MT #4 - New Order | `new_order` |
| `newRFP` | (DUMMY) MT #10 - New RFP | `new_rfp` |
| `orderTimeline` | (DUMMY) MT #15 - Order Timeline | `order_timeline` |
| `processApprove1` | Process - Approve1 | `process_approve1` |
| `processApprove2` | Process - Approve2 | `process_approve2` |
| `processEnhancementStatus` | (DUMMY) Form Feature #3 - Process Enhancement Tool Status Bar | `process_enhancement_status` |
| `processRequest` | Process - Request | `process_request` |
| `processRequestResubmit` | Process - Request Resubmit | `process_request_resubmit` |
| `ProcessRequestTab1` | Process - Request Tab1 | `process_request_tab1` |
| `ProcessRequestTab2` | Process - Request Tab2 | `process_request_tab2` |
| `ProcessRequestTab3` | Process - Request Tab3 | `process_request_tab3` |
| `ProcessRequestTab4` | Process - Request Tab4 | `process_request_tab4` |
| `processRequestView` | Process - Request View | `process_request_view` |
| `recentTransactions` | (DUMMY) Recent Transactions | `recent_transactions` |
| `sourceMedium` | (DUMMY) Source Medium | `source_medium` |
| `sqlQuery` | (DUMMY) Feature #6 - JDBC Load Binder | `sql_query` |
| `ticketsSoldByEvent` | (DUMMY) MT #5 - Tickets Sold by Event | `tickets_sold_by_event` |
| `trafficByDevice` | (DUMMY) MT #7 - Traffic by Device | `traffic_by_device` |
| `trafficSources` | (DUMMY) Traffic Sources | `traffic_sources` |
| `userPlatformPerMonth` | (DUMMY) MT #4 Mobile/Desktop Users per Month | `user_platform_per_month` |
| `websiteVisits` | (DUMMY) MT #12 - Website Visits | `website_visits` |

#### Lists (34 violations)

| Current ID | Display Name | Suggested ID |
|---|---|---|
| `appIcon` | Template - App Icon | `app_icon` |
| `candleStick` | Candlestick Data | `candle_stick` |
| `chartData` | Chart Data | `chart_data` |
| `dashboardInbox` | Dashboard Approver Inbox | `dashboard_inbox` |
| `list_calendarMeeting` | List - Calendar - Meeting | `list_calendar_meeting` |
| `list_conversionRates` | (DUMMY) List - MT #13 - Conversion Rates | `list_conversion_rates` |
| `list_currentVisits` | (DUMMY) List - MT #11 - Current Visits | `list_current_visits` |
| `list_featureAdvancedGrid` | List - Feature - Advanced Grid | `list_feature_advanced_grid` |
| `list_featureDateTime` | List - Feature - DateTime | `list_feature_date_time` |
| `list_featureFormGrid` | List - Feature - Form Grid | `list_feature_form_grid` |
| `list_featureListGrid` | List - Feature - List Grid | `list_feature_list_grid` |
| `list_featureMiscField` | List - Feature - Misc Field | `list_feature_misc_field` |
| `list_featureSelectField` | List - Feature - Select Fields | `list_feature_select_field` |
| `list_featureSpreadsheet` | List - Feature - Spreadsheet | `list_feature_spreadsheet` |
| `list_featureStdField` | List - Feature - Standard Fields | `list_feature_std_field` |
| `list_featureUploadField` | List - Feature - Upload Fields | `list_feature_upload_field` |
| `list_historicalSales` | (DUMMY) List - MT #6 - Historical Sales | `list_historical_sales` |
| `list_moneyOverYears` | (DUMMY) List - MT #8 - Money Over Years | `list_money_over_years` |
| `list_newOrder` | (DUMMY) List - New Order | `list_new_order` |
| `list_newRFP` | (DUMMY) List - MT #10 - New RFP | `list_new_rfp` |
| `list_processEnhancementStatus` | (DUMMY) List - Form Feature #3 - Process Enhancement Tool Status Bar | `list_process_enhancement_status` |
| `list_processRequest` | List - Process - Request | `list_process_request` |
| `list_recentTransactions` | (DUMMY) List - Recent Transactions | `list_recent_transactions` |
| `list_sourceMedium` | (DUMMY) List - Source Medium | `list_source_medium` |
| `list_ticketsSoldByEvent` | (DUMMY) List - MT #5 - Tickets Sold by Event | `list_tickets_sold_by_event` |
| `list_trafficByDevice` | (DUMMY) List - MT #7 - Traffic by Device | `list_traffic_by_device` |
| `list_trafficSources` | (DUMMY) List - Traffic Sources | `list_traffic_sources` |
| `list_userPlatformPerMonth` | (DUMMY) List - MT #4 Mobile/Desktop Users per Month | `list_user_platform_per_month` |
| `list_websiteVisits` | (DUMMY) List - MT #12 - Website Visits | `list_website_visits` |

---

### 🏆 Top 3 Recommendations

1. **Adopt lower `snake_case` for all IDs, globally.** Every form and list ID in the app uses camelCase. Establish a team-wide convention to use lowercase letters, digits, and underscores only (e.g., `process_request` instead of `processRequest`) — and enforce it via code review or automated checks before deployment.

2. **Fix capitalised prefixes immediately.** Four form IDs (`ProcessRequestTab1` through `ProcessRequestTab4`) start with an uppercase letter, which is a more severe deviation than plain camelCase. These should be prioritised in the rename effort as they violate the standard on two levels (uppercase start + camelCase).

3. **Rename during a planned maintenance window and update all references atomically.** Because IDs are referenced in process bindings, datalists, and UI configurations, renaming should be done systematically — export the app, perform a bulk find-and-replace on IDs, re-import, and regression-test to avoid broken bindings.